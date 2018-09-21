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

import alma.obops.draws.messages.TestUtils.TestMessage;
import alma.obops.draws.messages.couchdb.CouchDbMessageBus;

public class TestMessageQueue {

	private static final String QUEUE_NAME = "Q";
	private static TestMessage testMessage; 	// needs to be static!
	private MessageQueue queue = null;
	private final TestMessage jimi = new TestMessage( "Jimi Hendrix", 28, false );
	
	@Before
	public void aaa_setUp() throws IOException {
		MessageBus messageBus = new CouchDbMessageBus( COUCHDB_URL, null, null, MESSAGE_BUS_NAME  );
		DbConnection db = ((CouchDbMessageBus) messageBus).getDbConnection(); 
		db.dbDelete( MESSAGE_BUS_NAME );
		db.dbCreate( MESSAGE_BUS_NAME );
		this.queue = messageBus.messageQueue( QUEUE_NAME );
	}
	
	@Test
	public void construction() {
		assertNotNull( queue );
		assertEquals( QUEUE_NAME, queue.getName() );
//		assertTrue( messageBus == queue.getMessageBus() );
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
		Envelope[] out = queue.find( s );
		assertNotNull( out );
		assertFalse( out.length == 0 );
		Envelope outMessage = out[0];
		System.out.println( outMessage );
		assertEquals( jimi, outMessage.getMessage() );
	}

	@Test
	public void sendAndFindNext() throws Exception {
		queue.send( jimi );

		Envelope out = queue.findNext();
		assertNotNull( out );
		System.out.println( ">>> sendAndReceive(): out=" + out );
		assertEquals( jimi, out.getMessage() );
	}
	
	@Test
	// Send and find on two separate threads
	public void sendAndFindNextConcurrent() throws Exception {
		TestMessage in = new TestMessage( "Freddie Mercury", 45, false );
		
		Runnable sender = () -> {	
			queue.send( in );
			System.out.println( ">>> Sent: " + in );
		};
		Thread senderT = new Thread( sender );
		
		Runnable receiver = () -> {	
			try {
				Envelope next = (Envelope) queue.findNext();
				testMessage = (TestMessage) next.getMessage();
			} 
			catch ( Exception e ) {
				throw new RuntimeException( e );
			}
			System.out.println( ">>> Received: " + testMessage );
		};
		Thread receiverT = new Thread( receiver );
		
		testMessage = null;
		
		// Now run the threads
		receiverT.start();
		MessageBus.sleep( 2000 );
		senderT.start();
		senderT.join();
		receiverT.join();
		
		assertNotNull( testMessage );
		assertEquals( in, testMessage );
	}
	
	@Test
	// Send and receive on two separate threads -- uses listenInThread()
	public void sendAndListenInThread() throws Exception {
		
		TestMessage in = new TestMessage( "Freddie Mercury", 45, false );
		
		// Define a sender thread
		Runnable sender = () -> {	
			queue.send( in );
			System.out.println( ">>> Sent: " + in );
		};
		Thread senderT = new Thread( sender );
		
		// Define a consumer
		MessageConsumer mc = (message) -> {
			testMessage = (TestMessage) message;
			System.out.println( ">>> Received: " + message );
		};
		Thread receiverT = queue.listenInThread( mc, 3000, false, true );
	
		// Launch a listener with that consumer on a background thread
		testMessage = null;
		
		// Wait a bit, then launch the sender
		MessageBus.sleep( 1500 );
		senderT.start();
		
		// Wait for the threads to be done, then check that they 
		// exchanged the message
		senderT.join();
		receiverT.join();
		assertNotNull( testMessage );
		assertEquals( in, testMessage );
	}

	@Test
	// Send and receive on two separate threads
	public void sendAndReceiveSeparateThreads() throws Exception {
		
		TestMessage in = new TestMessage( "Freddie Mercury", 45, false );
		
		// Define a sender thread
		Runnable sender = () -> {	
			queue.send( in );
			System.out.println( ">>> Sent: " + in );
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
				queue.listen( mc, 3000, false, true );
			} 
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		};
		Thread receiverT = new Thread( receiver );

		// Launch a listener with that consumer on a background thread
		testMessage = null;
		receiverT.start();
		
		// Wait a bit, then launch the sender
		MessageBus.sleep( 1500 );
		senderT.start();
		
		// Wait for the threads to be done, then check that they 
		// exchanged the message
		senderT.join();
		receiverT.join();
		assertNotNull( testMessage );
		assertEquals( in, testMessage );
	}
}
