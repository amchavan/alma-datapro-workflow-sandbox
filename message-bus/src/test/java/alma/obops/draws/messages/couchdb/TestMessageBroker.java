package alma.obops.draws.messages.couchdb;

import static alma.obops.draws.messages.TestUtils.MESSAGE_BUS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import alma.obops.draws.messages.DbConnection;
import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.TestUtils.TestMessage;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.configuration.CouchDbConfiguration;
import alma.obops.draws.messages.configuration.CouchDbConfigurationProperties;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CouchDbConfiguration.class, CouchDbConfigurationProperties.class})
@AutoConfigureJdbc
public class TestMessageBroker {

	private static final String QUEUE_NAME = "Q";
	private static TestMessage testMessage; 	// needs to be static!
	private CouchDbMessageBroker messageBus = null;
	private final TestMessage jimi = new TestMessage( "Jimi Hendrix", 28, false );
	private DbConnection db;
	private MessageQueue queue;
	
	@Autowired
	private CouchDbConnection couchDbConn;
	
	@Before
	public void aaa_setUp() throws IOException {

		assertNotNull( couchDbConn );
		
		messageBus = new CouchDbMessageBroker( couchDbConn, MESSAGE_BUS_NAME  );
		queue = new MessageQueue( QUEUE_NAME, messageBus );
		db = ((CouchDbMessageBroker) messageBus).getDbConnection(); 
		db.dbDelete( MESSAGE_BUS_NAME );
		db.dbCreate( MESSAGE_BUS_NAME );
	}

	@Test
	public void construction() {
		MessageQueue queue = messageBus.messageQueue( QUEUE_NAME );
		assertNotNull( queue );
		assertEquals( QUEUE_NAME, queue.getName() );
		assertTrue( messageBus == queue.getMessageBroker() );
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
		DbConnection db = ((CouchDbMessageBroker) messageBus).getDbConnection();
		assertNotNull( db );
	}
	
	@Test
	public void messageRecordConstructorGeneratesID() {
		Envelope mr = new CouchDbEnvelope( jimi, null, null, 0 );
		assertNotNull( mr.getId() );
		System.out.println( mr.getId() );
	}
	
	@Test
	public void nowISO() {
		String nowISO = MessageBroker.nowISO();
		assertNotNull( nowISO );
		System.out.println( nowISO );
	}

	@Test
	public void ourIP() {
		String ourIP = MessageBroker.ourIP();
		assertNotNull( ourIP );
		assertFalse( ourIP.equals( "0.0.0.0" ));
		System.out.println( ">>> our IP: " + ourIP );		
	}

	
	@Test
	public void sendAndFind() throws Exception {
		Envelope in = messageBus.send( queue, jimi );
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
	public void sendAndReceive() throws Exception {
		messageBus.send( queue, jimi );

		Envelope out = messageBus.receive( queue );
		assertNotNull( out );
		System.out.println( ">>> sendAndReceive(): out=" + out );
		assertEquals( jimi, out.getMessage() );
	}
	
	@Test
	// Send and find on two separate threads
	public void sendAndreceiveNextConcurrent() throws Exception {
		TestMessage in = new TestMessage( "Freddie Mercury", 45, false );
		
		Runnable sender = () -> {	
			messageBus.send( queue, in );
			System.out.println( ">>> Sent: " + in );
		};
		Thread senderT = new Thread( sender );
		
		Runnable receiver = () -> {	
			try {
				Envelope next = (Envelope) messageBus.receive( queue );
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
		MessageBroker.sleep( 2000 );
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
			messageBus.send( queue, in );
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
				messageBus.listen( queue, mc, 3000 );
			}
			catch( TimeLimitExceededException e ) {
				System.out.println( ">>> timing out" );
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
		MessageBroker.sleep( 1500 );
		senderT.start();
		
		// Wait for the threads to be done, then check that they 
		// exchanged the message
		senderT.join();
		receiverT.join();
		assertNotNull( testMessage );
		assertEquals( in, testMessage );
	}
}
