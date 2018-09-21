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
import alma.obops.draws.messages.couchdb.CouchDbEnvelope;
import alma.obops.draws.messages.couchdb.CouchDbMessageBus;

public class TestMessageBus {

	private static final String QUEUE_NAME = "Q";
	private static TestMessage testMessage; 	// needs to be static!
	private MessageBus messageBus = null;
	private final TestMessage jimi = new TestMessage( "Jimi Hendrix", 28, false );
	private DbConnection db;
	
	@Before
	public void aaa_setUp() throws IOException {
		messageBus = new CouchDbMessageBus( COUCHDB_URL, null, null, MESSAGE_BUS_NAME  );
		db = ((CouchDbMessageBus) messageBus).getDbConnection(); 
		db.dbDelete( MESSAGE_BUS_NAME );
		db.dbCreate( MESSAGE_BUS_NAME );
	}

	@Test
	public void messageRecordConstructorGeneratesID() {
		Envelope mr = new CouchDbEnvelope( null, null, null, null );
		assertNotNull( mr.getId() );
		System.out.println( mr.getId() );
	}
	
	@Test
	public void joinGroup() throws IOException {
		
		String groupName = "recipients.*";
		String member1 = "recipient.test1";
		messageBus.joinGroup( member1, groupName );
		
		List<String> members = messageBus.groupMembers( groupName );
		assertNotNull( members );
		assertEquals( 1, members.size() );
		assertTrue( members.contains( member1 ));
		
		String member2 = "recipient.test2";
		messageBus.joinGroup( member2, groupName );
		members = messageBus.groupMembers( groupName );
		assertEquals( 2, members.size() );
		assertTrue( members.contains( member2 ));
	}
	
	@Test
	public void messageQueueConstructorGeneratesDbConnection() {
		DbConnection db = ((CouchDbMessageBus) messageBus).getDbConnection();
		assertNotNull( db );
	}
	
	@Test
	public void construction() {
		MessageQueue queue = messageBus.messageQueue( QUEUE_NAME );
		assertNotNull( queue );
		assertEquals( QUEUE_NAME, queue.getName() );
		assertTrue( messageBus == queue.getMessageBus() );
	}
	
	@Test
	public void nowISO() {
		String nowISO = MessageBus.nowISO();
		assertNotNull( nowISO );
		System.out.println( nowISO );
	}

	@Test
	public void ourIP() {
		String ourIP = MessageBus.ourIP();
		assertNotNull( ourIP );
		assertFalse( ourIP.equals( "0.0.0.0" ));
		System.out.println( ">>> our IP: " + ourIP );		
	}

	
	@Test
	public void sendAndFind() throws Exception {
		Envelope in = messageBus.send( QUEUE_NAME, jimi );
		assertNotNull( in );
		assertEquals( jimi, in.getMessage() );
		
		String s = "{ 'selector':{ '_id':{ '$gt':null }}}".replace( '\'', '\"' );
		Envelope[] out = messageBus.find( s );
		assertNotNull( out );
		assertFalse( out.length == 0 );
		Envelope outMessage = out[0];
		System.out.println( outMessage );
		assertEquals( jimi, outMessage.getMessage() );
	}

	@Test
	public void sendAndFindNext() throws Exception {
		messageBus.send( QUEUE_NAME, jimi );

		Envelope out = messageBus.findNext( QUEUE_NAME );
		assertNotNull( out );
		System.out.println( ">>> sendAndReceive(): out=" + out );
		assertEquals( jimi, out.getMessage() );
	}
	
	@Test
	// Send and find on two separate threads
	public void sendAndFindNextConcurrent() throws Exception {
		TestMessage in = new TestMessage( "Freddie Mercury", 45, false );
		
		Runnable sender = () -> {	
			messageBus.send( QUEUE_NAME, in );
			System.out.println( ">>> Sent: " + in );
		};
		Thread senderT = new Thread( sender );
		
		Runnable receiver = () -> {	
			try {
				Envelope next = (Envelope) messageBus.findNext( QUEUE_NAME );
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
	// Send and receive on two separate threads
	public void sendAndReceiveSeparateThreads() throws Exception {
		
		TestMessage in = new TestMessage( "Freddie Mercury", 45, false );
		
		// Define a sender thread
		Runnable sender = () -> {	
			messageBus.send( QUEUE_NAME, in );
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
				messageBus.listen( QUEUE_NAME, mc, 3000, false, true );
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
