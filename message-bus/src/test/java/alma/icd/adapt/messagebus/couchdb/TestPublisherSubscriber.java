package alma.obops.draws.messages.couchdb;

import static alma.obops.draws.messages.TestUtils.MESSAGE_BUS_NAME;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import alma.obops.draws.messages.DbConnection;
import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.Publisher;
import alma.obops.draws.messages.Subscriber;
import alma.obops.draws.messages.TestUtils.TestMessage;
import alma.obops.draws.messages.configuration.CouchDbConfiguration;
import alma.obops.draws.messages.configuration.CouchDbConfigurationProperties;
import alma.obops.draws.messages.TimeLimitExceededException;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CouchDbConfiguration.class, CouchDbConfigurationProperties.class})
@AutoConfigureJdbc
public class TestPublisherSubscriber {

	private static final String QUEUE_NAME = "rock.stars";
	private static final String SERVICE_NAME = "local";
	private static TestMessage testMessage; 	// needs to be static!
//	private MessageQueue queue = null;
	private final TestMessage jimi = new TestMessage( "Jimi Hendrix", 28, false );
	private final TestMessage freddie = new TestMessage( "Freddie Mercury", 45, false );
	private CouchDbMessageBroker broker;
	
	@Autowired
	private CouchDbConnection couchDbConn;

	private Publisher publisher;
	private Subscriber subscriber;
	
	@Before
	public void aaa_setUp() throws IOException {
		broker = new CouchDbMessageBroker( couchDbConn, MESSAGE_BUS_NAME  );
		this.publisher = new Publisher( broker, QUEUE_NAME );
		this.subscriber = new Subscriber( broker, QUEUE_NAME, SERVICE_NAME );
		DbConnection db = ((CouchDbMessageBroker) broker).getDbConnection(); 
		db.dbDelete( MESSAGE_BUS_NAME );
		db.dbCreate( MESSAGE_BUS_NAME );
//		this.queue = broker.messageQueue( QUEUE_NAME );
		testMessage = null;			// reset every time	
	}


//	// SENDING TO GROUPS IS CURRENTLY BROKEN
//	// amchavan, 07-Jan-2019
//	//
//	@Test
//	public void joinGroup() throws IOException {
//		
//		String groupName = "recipients.*";
//		queue.joinGroup( groupName );
//		
//		List<String> members = queue.getMessageBroker().groupMembers( groupName );
//		assertNotNull( members );
//		assertEquals( 1, members.size() );
//		assertTrue( members.contains( queue.getName() ));
//	}

	@Test
	public void send_Receive() throws Exception {
		
		publisher.publish( jimi );
		Envelope out = subscriber.receive();
//		System.out.println( ">>> send_Receive(): out=" + out );
		
		assertNotNull( out );
		assertEquals( State.Received, out.getState() );
		assertNotNull( out.getSentTimestamp() );
		assertEquals( jimi, out.getMessage() );
		assertEquals( out, out.getMessage().getEnvelope() );
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
				testMessage  = (TestMessage) e.getMessage();
			} 
			catch ( Exception e ) {
				throw new RuntimeException( e );
			}
			System.out.println( ">>> Received: " + testMessage );
		};
		Thread receiverT = new Thread( receiver );
		
		// Now run the threads
		receiverT.start();
		MessageBroker.sleep( 2000 );
		senderT.start();
		senderT.join();
		receiverT.join();
		
		assertNotNull( testMessage );
		assertEquals( freddie, testMessage );
		assertEquals( State.Received, testMessage.getEnvelope().getState() );
		assertNotNull( testMessage.getEnvelope().getReceivedTimestamp() );
	}
	
	@Test
	// Send and receive on two separate threads -- uses listenInThread()
	public void send_ListenInThread() throws Exception {
		
		// Define a sender thread
		Runnable sender = () -> {	
			publisher.publish( jimi );
			System.out.println( ">>> Sent: " + jimi );
		};
		Thread senderT = new Thread( sender );
		
		// Define a consumer
		MessageConsumer mc = (message) -> {
			testMessage = (TestMessage) message;
			System.out.println( ">>> Received: " + message );
		};

		// Launch a listener with that consumer on a background thread
		Thread receiverT = subscriber.listenInThread( mc, 3000 );
	
		// Wait a bit, then launch the sender
		MessageBroker.sleep( 1500 );
		senderT.start();
		
		// Wait for the threads to be done, then check that they 
		// exchanged the message
		senderT.join();
		receiverT.join();
		assertNotNull( testMessage );
		assertEquals( jimi, testMessage );
		assertEquals( State.Consumed, testMessage.getEnvelope().getState() );
		assertNotNull( testMessage.getEnvelope().getConsumedTimestamp() );
	}

	@Test
	// Send and receive on two separate threads
	public void send_ReceiveSeparateThreads() throws Exception {
		
		// Define a sender thread
		Runnable sender = () -> {	
			publisher.publish( jimi );
			System.out.println( ">>> Sent: " + jimi );
		};
		Thread senderT = new Thread( sender );
		
		// Define a consumer
		MessageConsumer mc = (message) -> {
			testMessage = (TestMessage) message;
			System.out.println( ">>> Received: " + message );
		};
		
		// Define a sender thread
		Runnable receiver = () -> {	
			try {
				subscriber.listen( mc, 3000 );
			}
			catch( TimeLimitExceededException e ) {
				// no-op, expected
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		};
		Thread receiverT = new Thread( receiver );

		// Launch a listener with that consumer on a background thread
		receiverT.start();
		
		// Wait a bit, then launch the sender
		MessageBroker.sleep( 1500 );
		senderT.start();
		
		// Wait for the threads to be done, then check that they 
		// exchanged the message
		senderT.join();
		receiverT.join();
		assertNotNull( testMessage );
		assertEquals( jimi, testMessage );
		assertEquals( State.Consumed, testMessage.getEnvelope().getState() );
		assertNotNull( testMessage.getEnvelope().getConsumedTimestamp() );
	}

	@Test
	public void send_LetExpire() throws Exception {
		
		Envelope e = publisher.publish( jimi, 1000L );			
		System.out.println( ">>> Sent: " + e );

		e = publisher.publish( freddie );
		System.out.println( ">>> Sent: " + e );

		MessageBroker.sleep( 1100 );	// Let the first message expire
	
		MessageConsumer mc = (message) -> {
			testMessage = (TestMessage) message;
			System.out.println( ">>> Received: " + message );
		};
		
		try {
			subscriber.listen( mc, 50 );	// terminate after timeout
		} 
		catch( TimeLimitExceededException te ) {
			// no-op, expected
		}

		assertNotNull( testMessage );
		assertEquals( freddie, testMessage );
		
		String s = "{ 'selector':{ 'state':'Expired' }}".replace( '\'', '\"' );
		Envelope[] found = broker.find( s );
		assertEquals( 1, found.length );
		assertEquals( State.Expired, found[0].getState() );
		assertNotNull( found[0].getExpiredTimestamp() );
		assertEquals( jimi, found[0].getMessage() );
	}

	@Test
	public void send_LetExpire_Purge() throws Exception {
		
		publisher.publish( jimi, 1000L );	
		publisher.publish( freddie, 1000L );
	
		MessageBroker.sleep( 1100 );	// Let both messages expire
	
		int purged = broker.expireMessages( QUEUE_NAME );
		assertEquals( 2, purged );
	}

// SENDING TO GROUPS IS CURRENTLY BROKEN
// amchavan, 07-Jan-2019
//
//	@Test
//	public void sendToGroup() throws Exception {
//
//		System.out.println( ">>> sendToGroup ========================================" );
//
//		// Create the recipient group
//		final String groupName = "recipients.*";
//		
//		queue.joinGroup( groupName );
//		queue2.joinGroup( groupName );
//		
//		
//		// Send to the recipient group
//		Envelope e = publisher.publish( jimi );			
//		System.out.println( ">>> Sent to group: " + e );
//
//		Envelope out1 = queue.receive();
//		assertNotNull( out1 );
//		assertEquals( jimi, out1.getMessage() );
//		assertEquals( QUEUE_NAME, out1.getQueueName() );
//
//		Envelope out2 = queue2.receive();
//		assertNotNull( out2 );
//		assertEquals( jimi, out2.getMessage() );
//		assertEquals( QUEUE_NAME_2, out2.getQueueName() );
//	}
}
