package alma.obops.draws.messages;

import static alma.obops.draws.messages.TestUtils.COUCHDB_URL;
import static alma.obops.draws.messages.TestUtils.MESSAGE_BUS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.TestUtils.TestMessage;
import alma.obops.draws.messages.couchdb.CouchDbMessageBus;

public class TestMessageQueue {

	private static final String QUEUE_NAME = "Q";
	private static TestMessage testMessage; 	// needs to be static!
	private MessageQueue queue = null;
	private final TestMessage jimi = new TestMessage( "Jimi Hendrix", 28, false );
	private TestMessage freddie = new TestMessage( "Freddie Mercury", 45, false );
	private MessageBus messageBus;
	
	@Before
	public void aaa_setUp() throws IOException {
		messageBus = new CouchDbMessageBus( COUCHDB_URL, null, null, MESSAGE_BUS_NAME  );
		DbConnection db = ((CouchDbMessageBus) messageBus).getDbConnection(); 
		db.dbDelete( MESSAGE_BUS_NAME );
		db.dbCreate( MESSAGE_BUS_NAME );
		this.queue = messageBus.messageQueue( QUEUE_NAME );
		testMessage = null;			// reset every time
	}
	
	@Test
	public void construction() {
		assertNotNull( queue );
		assertEquals( QUEUE_NAME, queue.getName() );
		assertTrue( messageBus == queue.getMessageBus() );
	}

	@Test
	public void joinGroup() throws IOException {
		
		String groupName = "recipients.*";
		queue.joinGroup( groupName );
		
		List<String> members = queue.getMessageBus().groupMembers( groupName );
		assertNotNull( members );
		assertEquals( 1, members.size() );
		assertTrue( members.contains( queue.getName() ));
	}
	
	@Test
	public void sendAndFind() throws Exception {
		Envelope in = queue.send( jimi );
		assertNotNull( in );
		assertEquals( jimi, in.getMessage() );
		
		String s = "{ 'selector':{ '_id':{ '$gt':null }}}".replace( '\'', '\"' );
		Envelope[] found = queue.find( s );
		assertNotNull( found );
		assertFalse( found.length == 0 );
		Envelope out = found[0];
//		System.out.println( out );
		
		assertEquals( State.Sent, out.getState() );
		assertNotNull( out.getSentTimestamp() );
		assertEquals( jimi, out.getMessage() );
	}

	@Test
	public void sendAndReceive() throws Exception {
		queue.send( jimi );

		Envelope out = queue.receive();
//		System.out.println( ">>> sendAndReceive(): out=" + out );
		
		assertNotNull( out );
		assertEquals( State.Received, out.getState() );
		assertNotNull( out.getSentTimestamp() );
		assertEquals( jimi, out.getMessage() );
		assertEquals( out, out.getMessage().getEnvelope() );
	}
	
	@Test
	// Send and find on two separate threads
	public void sendAndReceiveConcurrent() throws Exception {
		Runnable sender = () -> {	
			queue.send( freddie );
			System.out.println( ">>> Sent: " + freddie );
		};
		Thread senderT = new Thread( sender );
		
		Runnable receiver = () -> {	
			try {
				Envelope e = (Envelope) queue.receive();
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
		MessageBus.sleep( 2000 );
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
	public void sendAndListenInThread() throws Exception {
		
		// Define a sender thread
		Runnable sender = () -> {	
			queue.send( jimi );
			System.out.println( ">>> Sent: " + jimi );
		};
		Thread senderT = new Thread( sender );
		
		// Define a consumer
		MessageConsumer mc = (message) -> {
			testMessage = (TestMessage) message;
			System.out.println( ">>> Received: " + message );
		};

		// Launch a listener with that consumer on a background thread
		Thread receiverT = queue.listenInThread( mc, 3000, true );
	
		
		// Wait a bit, then launch the sender
		MessageBus.sleep( 1500 );
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
	public void sendAndReceiveSeparateThreads() throws Exception {
		
		// Define a sender thread
		Runnable sender = () -> {	
			queue.send( jimi );
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
				queue.listen( mc, 3000, true );
			} 
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		};
		Thread receiverT = new Thread( receiver );

		// Launch a listener with that consumer on a background thread
		receiverT.start();
		
		// Wait a bit, then launch the sender
		MessageBus.sleep( 1500 );
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
	public void sendAndLetExpire() throws Exception {
		
		MessageQueue queue = messageBus.messageQueue( QUEUE_NAME );
		Envelope e = queue.send( jimi, 1000L );			
		System.out.println( ">>> Sent: " + e );

		e = queue.send( freddie );
		System.out.println( ">>> Sent: " + e );

		MessageBus.sleep( 1100 );	// Let the first message expire
	
		MessageConsumer mc = (message) -> {
			testMessage = (TestMessage) message;
			System.out.println( ">>> Received: " + message );
		};
		
		try {
			queue.listen( mc, 50, false );	// terminate after timeout
		} 
		catch( TimeoutException te ) {
			// no-op, expected
		}

		assertNotNull( testMessage );
		assertEquals( freddie, testMessage );
		
		String s = "{ 'selector':{ 'state':'Expired' }}".replace( '\'', '\"' );
		Envelope[] found = queue.find( s );
		assertEquals( 1, found.length );
		assertEquals( State.Expired, found[0].getState() );
		assertNotNull( found[0].getExpiredTimestamp() );
		assertEquals( jimi, found[0].getMessage() );
	}

	@Test
	public void sendLetExpireAndPurge() throws Exception {
		
		MessageQueue queue = messageBus.messageQueue( QUEUE_NAME );
		queue.send( jimi, 1000L );	
		queue.send( freddie, 1000L );
	
		MessageBus.sleep( 1100 );	// Let both messages expire
	
		int purged = messageBus.purgeExpiredMessages( QUEUE_NAME );
		assertEquals( 2, purged );
	}
}
