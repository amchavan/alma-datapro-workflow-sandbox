package alma.obops.draws.messages.rabbitmq;

import static alma.obops.draws.messages.TestUtils.RABBITMQ_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.TestUtils.TestMessage;
import alma.obops.draws.messages.TimeLimitExceededException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PersistenceConfiguration.class)
@AutoConfigureJdbc
public class TestMessageQueue {

	private static final String QUEUE_NAME = "test.queue";
	private static final String EXCHANGE_NAME = "unit-test-exchange";

	private final TestMessage jimi = new TestMessage( "Jimi Hendrix", 28, false );
	private final TestMessage freddie = new TestMessage( "Freddie Mercury", 45, false );
	private final TestMessage brian = new TestMessage( "Brian May", 71, true );
	private RabbitMqMessageBroker broker;
	private MessageQueue queue;
	private Message receivedMessage;

	@Autowired
	private PersistedEnvelopeRepository envelopeRepository;
	
	@Autowired
	private RecipientGroupRepository groupRepository;
	
	@Before
	public void aaa_setUp() throws Exception {

		System.out.println( ">>> SETUP ========================================" );
		
		this.broker = new RabbitMqMessageBroker( RABBITMQ_URL, EXCHANGE_NAME,
												 envelopeRepository, groupRepository );
		this.queue = broker.messageQueue( QUEUE_NAME );

		// Drain any existing messages in the logging queue
		broker.drainLoggingQueue();
		
		// Drain the queue 
		while( true ) {
			Envelope e;
			try {
				e = queue.receive( 1000L );
			}
			catch ( TimeLimitExceededException e1) {
				// no-op, expected when the queue is empty;
				break;
			}
			System.out.println( ">>> Draining: found: " +  e.getMessage() );
		}
		
		envelopeRepository.deleteAll();
		groupRepository.deleteAll();
	}

	@After
	public void aaa_tearDown() throws Exception {
		broker.deleteQueue( this.queue );
	}
	
	@Test
	public void joinGroup() throws IOException {
		
		String groupName = "recipients.*";
		queue.joinGroup( groupName );
		
		List<String> members = queue.getMessageBroker().groupMembers( groupName );
		assertNotNull( members );
		assertEquals( 1, members.size() );
		assertTrue( members.contains( queue.getName() ));
	}
	
	@Test
	public void send_LetExpire_Listen() throws Exception {

		// Drain any existing messages in the logging queue
		broker.drainLoggingQueue();
		
		Envelope e = queue.send( jimi, 1000L );				// jimi will expire
		System.out.println( ">>> Sent: " + e );
	
		e = queue.send( freddie );							// freddie will not expire
		System.out.println( ">>> Sent: " + e );
	
		MessageBroker.sleep( 1100 );	// Let the first message expire
	
		MessageConsumer mc = (message) -> {
			setReceivedMessage( message );
			System.out.println( ">>> Received: " + message );
		};
		
		try {
			queue.listen( mc, 50, false );	// terminate after timeout
		} 
		catch( TimeLimitExceededException te ) {
			// no-op, expected
		}
	
		assertNotNull( receivedMessage );
		assertEquals( freddie, receivedMessage );
		
		// Process all pending log messages because we need to 
		// interrogate the database
		Runnable messageLogListener = broker.getMessageLogListener();
		Thread messageLogThread = new Thread( messageLogListener );
		messageLogThread.start();
		MessageBroker.sleep( 1000L );	// Give some time to the background thread to catch up
		messageLogThread.join();
		
		for( PersistedEnvelope pe: envelopeRepository.findAll() ) {
			System.out.println( ">>> " + pe );
		}
		
		List<PersistedEnvelope> found = envelopeRepository.findByState( "Expired" );
		assertEquals( 1, found.size() );
		Envelope expired = found.get(0).asSimpleEnvelope();
		assertEquals( State.Expired, expired.getState() );
		assertNotNull( expired.getExpiredTimestamp() );
		assertEquals( jimi, expired.getMessage() );
		

		found = envelopeRepository.findByState( "Consumed" );
		assertEquals( 1, found.size() );
		Envelope consumed = found.get(0).asSimpleEnvelope();
		assertEquals( State.Consumed, consumed.getState() );
		assertNotNull( consumed.getConsumedTimestamp() );
		assertEquals( jimi, expired.getMessage() );
	}
	
	@Test
	// Send and receive on two separate threads -- uses listenInThread()
	public void send_ListenInThread() throws Exception {
		
		// Define a sender thread
		Runnable sender = () -> {	
			queue.send( jimi );
			System.out.println( ">>> Sent: " + jimi );
		};
		Thread senderT = new Thread( sender );
		
		// Define a consumer
		MessageConsumer mc = (message) -> {
			receivedMessage = (TestMessage) message;
			System.out.println( ">>> Received: " + message );
		};

		// Launch a listener with that consumer on a background thread
		Thread receiverT = queue.listenInThread( mc, 3000, true );
	
		
		// Wait a bit, then launch the sender
		MessageBroker.sleep( 1500 );
		senderT.start();
		
		// Wait for the threads to be done, then check that they 
		// exchanged the message
		senderT.join();
		receiverT.join();
		assertNotNull( receivedMessage );
		assertEquals( jimi, receivedMessage );
		assertEquals( State.Consumed, receivedMessage.getEnvelope().getState() );
		assertNotNull( receivedMessage.getEnvelope().getConsumedTimestamp() );
	}

	@Test
	public void send_Receive() throws IOException, InterruptedException {
		
//		Thread receiverThread = new Thread( receiver );
//		receiverThread.start();
		queue.send( jimi );
		Envelope out = queue.receive();
//		receiverThread.join();
		
//		System.out.println( ">>> sendAndReceive(): out=" + out );
		
		assertNotNull( out );
		assertEquals( State.Received, out.getState() );
		assertNotNull( out.getSentTimestamp() );
		assertEquals( jimi, out.getMessage() );
		assertEquals( out, out.getMessage().getEnvelope() );
	}

	@Test
	public void send_Receive_Log() throws Exception {

		System.out.println( ">>> Send/Receive/Log ========================================" );

		// Drain any existing messages in the logging queue
		broker.drainLoggingQueue();

		Envelope sent = queue.send( brian );			
		System.out.println( ">>> sent: " + sent  );
		
		@SuppressWarnings("unused")
		Envelope received = queue.receive();

		Runnable messageLogListener = broker.getMessageLogListener();
		Thread messageLogThread = new Thread( messageLogListener );
		messageLogThread.start();
		messageLogThread.join();
		// Give some time to the background thread to catch up
		MessageBroker.sleep( 1000L );
		
		Iterable<PersistedEnvelope> foundEnvelopes = envelopeRepository.findAll();
		List<PersistedEnvelope> persistedEnvelopes = new ArrayList<>();
		for (PersistedEnvelope pe : foundEnvelopes) {
			persistedEnvelopes.add( pe );
		}
		
		assertEquals( 1, persistedEnvelopes.size() );
		PersistedEnvelope persistedEnvelope = persistedEnvelopes.get(0);
		Envelope foundEnvelope = persistedEnvelope.asSimpleEnvelope();
		assertEquals( sent.getId(),      foundEnvelope.getId() );
		assertEquals( sent.getMessage(), foundEnvelope.getMessage() );
		assertEquals( State.Received,    foundEnvelope.getState() );
	}
	


	@Test
	public void send_Receive_Multiple() throws Exception {
	
		System.out.println( ">>> Send/Receive multiple ========================================" );
		final int repeats = 10;
		
		for( int i = 0; i < repeats; i++ ) {
			String queueName = QUEUE_NAME + i;
			MessageQueue queue = broker.messageQueue( queueName );
			TestMessage message = new TestMessage( null, i, true );
			Envelope sent = queue.send( message );
			System.out.println( ">>> sent: " + sent );
		}
		
		
		for( int i = repeats-1; i>=0 ; i-- ) {
			String queueName = QUEUE_NAME + i;
			MessageQueue queue = broker.messageQueue( queueName );
			Envelope received = queue.receive( 2000L );
			System.out.println( ">>> received: " + received );
			TestMessage message = (TestMessage) received.getMessage();
			assertEquals( i, message.age );
			broker.deleteQueue( queue );
		}
	}

	@Test
	// Send and find on two separate threads
	public void send_ReceiveConcurrent() throws Exception {
		
		Runnable sender = () -> {	
			queue.send( freddie );
			System.out.println( ">>> Sent: " + freddie );
		};
		Thread senderT = new Thread( sender );
		
		Runnable receiver = () -> {	
			try {
				Envelope e = (Envelope) queue.receive();
				setReceivedMessage( (TestMessage) e.getMessage() );
			} 
			catch ( Exception e ) {
				e.printStackTrace();
				throw new RuntimeException( e );
			}
			System.out.println( ">>> Received: " + receivedMessage );
		};
		Thread receiverT = new Thread( receiver );
		
		// Now run the threads
		receiverT.start();
		MessageBroker.sleep( 2000 );
		senderT.start();
		senderT.join();
		receiverT.join();
		
		assertNotNull( receivedMessage );
		assertEquals( freddie, receivedMessage );
		assertEquals( State.Received, receivedMessage.getEnvelope().getState() );
		assertNotNull( receivedMessage.getEnvelope().getReceivedTimestamp() );
	}

	
	@Test
	public void sendToGroup() throws Exception {

		System.out.println( ">>> sendToGroup ========================================" );

		// Drain any existing messages in the logging queue
		broker.drainLoggingQueue();

		// Create the recipient group
		String groupName = "recipients.*";
		String queue2Name = QUEUE_NAME + "." + 2;
		MessageQueue queue2 = broker.messageQueue( queue2Name );
		MessageQueue group = broker.messageQueue( groupName );
		
		queue.joinGroup( groupName );
		queue2.joinGroup( groupName );
		
		// Send to the recipient group
		Envelope e = group.send( jimi );			
		System.out.println( ">>> Sent to group: " + e );

		Envelope out1 = queue.receive();
		assertNotNull( out1 );
		assertEquals( jimi, out1.getMessage() );

		Envelope out2 = queue2.receive();
		assertNotNull( out2 );
		assertEquals( jimi, out2.getMessage() );

		broker.deleteQueue( queue2 );
		broker.deleteQueue( group );
		
		// Process all pending log messages because we need to 
		// interrogate the database
		Runnable messageLogListener = broker.getMessageLogListener();
		Thread messageLogThread = new Thread( messageLogListener );
		messageLogThread.start();
		messageLogThread.join();
		// Give some time to the background thread to catch up
		MessageBroker.sleep( 2000L );
		
		for( PersistedEnvelope pe: envelopeRepository.findAll() ) {
			System.out.println( ">>> pe: " + pe );
		}
		
		List<PersistedEnvelope> found = envelopeRepository.findByState( "Received" );
		assertEquals( 2, found.size() );
		
		Envelope e0 = found.get(0).asSimpleEnvelope();
		assertEquals( jimi, e0.getMessage() );
		assertEquals( QUEUE_NAME, e0.getQueueName()  );
		
		Envelope e1 = found.get(1).asSimpleEnvelope();
		assertEquals( jimi, e1.getMessage() );
		assertEquals( queue2Name, e1.getQueueName()  );
	}

	private void setReceivedMessage( Message message ) {
		this.receivedMessage = message;
	}
}
