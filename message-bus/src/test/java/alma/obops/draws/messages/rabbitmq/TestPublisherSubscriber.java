package alma.obops.draws.messages.rabbitmq;

import static org.junit.Assert.*;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.Publisher;
import alma.obops.draws.messages.Subscriber;
import alma.obops.draws.messages.TestUtils.TestMessage;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.configuration.EmbeddedDataSourceConfiguration;
import alma.obops.draws.messages.configuration.PersistedEnvelopeRepository;
import alma.obops.draws.messages.configuration.PersistedRabbitMqBrokerConfiguration;
import alma.obops.draws.messages.configuration.PersistenceConfiguration;
import alma.obops.draws.messages.configuration.RabbitMqConfigurationProperties;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PersistenceConfiguration.class,
        PersistedRabbitMqBrokerConfiguration.class,
        RabbitMqConfigurationProperties.class,
        EmbeddedDataSourceConfiguration.class } )
@AutoConfigureJdbc
@ActiveProfiles( "unit-test-rabbitmq" )
public class TestPublisherSubscriber {

	private static final String QUEUE_NAME = "rock.stars";
	private static final String SERVICE_NAME = "local";

	private final TestMessage jimi = new TestMessage( "Jimi Hendrix", 28, false );
	private final TestMessage freddie = new TestMessage( "Freddie Mercury", 45, false );
	private final TestMessage brian = new TestMessage( "Brian May", 71, true );
	
	@Autowired
	private MessageBroker broker;
	
	private RabbitMqMessageBroker rmqBroker;
//	private MessageQueue queue;
	private Message receivedMessage;
	private PersistedEnvelopeRepository envelopeRepository;
	private Publisher publisher;
	private Subscriber subscriber;

	
	@Before
	public void aaa_setUp() throws Exception {

		System.out.println( ">>> SETUP ========================================" );
		this.publisher = new Publisher( broker, QUEUE_NAME );
		this.subscriber = new Subscriber( broker, QUEUE_NAME, SERVICE_NAME );
		
		this.rmqBroker = (RabbitMqMessageBroker) this.broker;
		this.rmqBroker.drainLoggingQueue();
		this.envelopeRepository = rmqBroker.getEnvelopeRepository();
		this.envelopeRepository.deleteAll();
	}

	@After
	public void aaa_tearDown() throws Exception {
		subscriber.getQueue().delete();
	}
	
	@Test
	// Listens until it times out, then it tries to listen again and this time it does work
	public void listenTimeout() throws Exception {
		
		// Define a consumer
		MessageConsumer mc = (message) -> {
			receivedMessage = (TestMessage) message;
			System.out.println( ">>> Received: " + message );
		};
		
		// First time around -- just timeout
		receivedMessage = null;
		try {
			subscriber.listen( mc, 1000 );
			MessageBroker.sleep( 2000 );
		} 
		catch( TimeLimitExceededException e ) {
			// no-op, expected
		}
		catch( Exception e ) {
			fail( e.getMessage() );
		}
		assertNull( receivedMessage );

		// Second time around: send a message, retrieve, timeout
		publisher.publish( jimi );
		System.out.println( ">>> Sent: " + jimi );
		
		try {
			subscriber.listen( mc, 1000 );
			MessageBroker.sleep( 2000 );
		} 
		catch( TimeLimitExceededException e ) {
			// no-op, expected
		}
		catch( Exception e ) {
			fail( e.getMessage() );
		}

		// Wait for the threads to be done
		MessageBroker.sleep( 500 );	
		
		assertNotNull( receivedMessage );
		assertEquals(  jimi, receivedMessage );
		assertEquals(  State.Consumed, receivedMessage.getEnvelope().getState() );
		assertNotNull( receivedMessage.getEnvelope().getConsumedTimestamp() );
	}
	
	@Test
	public void send_LetExpire_Listen() throws Exception {

		// Drain any existing messages in the logging queue
		rmqBroker.drainLoggingQueue();
		
		Envelope e;
		
		e = publisher.publish( jimi, 1000L );						// jimi will expire
		System.out.println( ">>> Sent: " + e );
		e = publisher.publish( freddie );							// freddie will not expire
		System.out.println( ">>> Sent: " + e );
	
		MessageBroker.sleep( 1100 );	// Let the first message expire
	
		MessageConsumer mc = (message) -> {
			setReceivedMessage( message );
			System.out.println( ">>> Received: " + message );
		};
		
		try {
			subscriber.listen( mc, 50 );	// terminate after timeout
		} 
		catch( TimeLimitExceededException te ) {
			// no-op, expected
		}
		finally {
			MessageBroker.sleep( 500L );	// Give some time to the background thread to catch up
		}
	
		assertNotNull( receivedMessage );
		assertEquals( freddie, receivedMessage );
		
		// Process all pending log messages because we need to 
		// interrogate the database
		Runnable messageLogListener = rmqBroker.getMessageArchiver();
		Thread messageLogThread = new Thread( messageLogListener );
		messageLogThread.start();			
		MessageBroker.sleep( 500L );	// Give some time to the background thread to catch up
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
			publisher.publish( jimi );
			System.out.println( ">>> published: " + jimi );
		};
		Thread senderT = new Thread( sender );
		
		// Define a consumer
		MessageConsumer mc = (message) -> {
			receivedMessage = (TestMessage) message;
			System.out.println( ">>> Received: " + message );
		};

		// Launch a listener with that consumer on a background thread
		// Launch the sender
		Thread receiverT = subscriber.listenInThread( mc, 3000 );
		senderT.start();

		// Wait for the threads to be done
		MessageBroker.sleep( 500 );	
		senderT.join();
		receiverT.join();
		
		assertNotNull( receivedMessage );
		assertEquals( jimi, receivedMessage );
		assertEquals( State.Consumed, receivedMessage.getEnvelope().getState() );
		assertNotNull( receivedMessage.getEnvelope().getConsumedTimestamp() );
	}

	@Test
	public void send_Receive() throws IOException, InterruptedException {

		publisher.publish( jimi );
		Envelope received = subscriber.receive();
		
		assertNotNull( received );
		assertEquals( State.Received, received.getState() );
		assertNotNull( received.getReceivedTimestamp() );
		assertEquals( jimi, received.getMessage() );
		assertEquals( received, received.getMessage().getEnvelope() );
	}

	@Test
	public void send_Receive_Log() throws Exception {

		// Drain any existing messages in the logging queue
		rmqBroker.drainLoggingQueue();

		Envelope published = publisher.publish( brian );			
		System.out.println( ">>> published: " + published  );
		
		@SuppressWarnings("unused")
		Envelope received = subscriber.receive();

		Runnable messageLogListener = rmqBroker.getMessageArchiver();
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
		assertEquals( published.getId(),      foundEnvelope.getId() );
		assertEquals( published.getMessage(), foundEnvelope.getMessage() );
		assertEquals( State.Received,    foundEnvelope.getState() );
	}

	@Test
	public void send_Receive_Multiple() throws Exception {
	
		System.out.println( ">>> Send/Receive multiple ========================================" );
		final int repeats = 10;
		
		for( int i = 0; i < repeats; i++ ) {	
			TestMessage message = new TestMessage( null, i, true );
			Envelope published = publisher.publish( message );
			System.out.println( ">>> published: " + published );
		}
		
		for( int i = 0; i < repeats; i++ ) {
			Envelope received = subscriber.receive( 2000 );
			System.out.println( ">>> received: " + received );
			TestMessage message = (TestMessage) received.getMessage();
			assertEquals( i, message.age );
		}
	}
	
	@Test
	// Send and find on two separate threads
	public void send_ReceiveConcurrent() throws Exception {
		
		Runnable sender = () -> {	
			publisher.publish( freddie );
			System.out.println( ">>> Sent: " + freddie );
		};
		Thread senderT = new Thread( sender );
		
		Runnable receiver = () -> {	
			try {
				Envelope e = (Envelope) subscriber.receive();
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

	private void setReceivedMessage( Message message ) {
		this.receivedMessage = message;
	}
}
