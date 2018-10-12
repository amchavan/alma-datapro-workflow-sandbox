package alma.obops.draws.messages.couchdb;

import static alma.obops.draws.messages.TestUtils.COUCHDB_URL;
import static alma.obops.draws.messages.TestUtils.MESSAGE_BUS_NAME;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import alma.obops.draws.messages.AbstractMessage;
import alma.obops.draws.messages.DbConnection;
import alma.obops.draws.messages.Executor;
import alma.obops.draws.messages.ExecutorClient;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.RequestMessage;
import alma.obops.draws.messages.RequestProcessor;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.couchdb.CouchDbMessageBroker;

public class TestExecutor {

	private static final String QUEUE_NAME = "DOUBLER_Q";
	static Integer globalDoubled = null; 			// needs to be static!
	private MessageBroker broker = null;
	private MessageQueue queue;

	// Request: double a number
	public static class DoubleRequest extends AbstractMessage implements RequestMessage  {
		public int number;

		public DoubleRequest() {
			// empty
		}

		public DoubleRequest( int number ) {
			this.number = number;
		}
	}

	// Response: a doubled number
	public static class DoubleResponse extends AbstractMessage {
		public int doubled;

		public DoubleResponse() {
			// empty
		}

		public DoubleResponse( int doubled ) {
			this.doubled = doubled;
		}
	}

	// Doubles its input
	public static class Doubler implements RequestProcessor {

		@Override
		public Message process(RequestMessage message) {

			DoubleRequest request = (DoubleRequest) message;
			System.out.println( ">>> Received request with number=" + request.number );
			int doubled = request.number + request.number;
			return new DoubleResponse( doubled );
		}
	}
	
	// This client sends a request to double a number to the queue; when the
	// response arrives the client copies it to a global variable to share it with
	// the rest of this test case
	public class BasicExecutorClient {
		public void request( int number ) throws IOException {

			MessageConsumer consumer = (message) -> {
				globalDoubled = ((DoubleResponse) message).doubled;
			};
			ExecutorClient client = new ExecutorClient( queue, consumer );
			DoubleRequest request = new DoubleRequest( number ); 

			client.call( request );
		}
	}
	
	@Before
	public void aaa_setUp() throws IOException {
		broker = new CouchDbMessageBroker( COUCHDB_URL, null, null, MESSAGE_BUS_NAME  );
		queue = broker.messageQueue( QUEUE_NAME );
		
		DbConnection db = ((CouchDbMessageBroker) broker).getDbConnection(); 
		db.dbDelete( MESSAGE_BUS_NAME );
		db.dbCreate( MESSAGE_BUS_NAME );
	}

	@Test
	public void testDoubler() throws Exception {

		RequestProcessor processor = new Doubler();
		Executor executor = new Executor( queue, processor, 5000 );
		BasicExecutorClient client = new BasicExecutorClient();

		// Define a sender thread
		Runnable receiver = () -> {	
			try {
				executor.run();
			} 
			catch (TimeLimitExceededException e) {
				System.out.println(">>> Timed out: " + e.getMessage());
			} 
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		};
		Thread doublerT = new Thread( receiver );
		doublerT.start();
		
		globalDoubled = null;
		client.request( 1 );
		System.out.println( ">>> Received reply with number: " + globalDoubled );
		assertEquals( 2, globalDoubled.intValue() );
		
		globalDoubled = null;
		client.request( 2 );
		System.out.println( ">>> Received reply with number: " + globalDoubled );
		assertEquals( 4, globalDoubled.intValue() );
		doublerT.join();
	}
}
