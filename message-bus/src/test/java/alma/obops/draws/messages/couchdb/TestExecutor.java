package alma.obops.draws.messages.couchdb;

import static alma.obops.draws.messages.TestUtils.MESSAGE_BUS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import alma.obops.draws.messages.AbstractMessage;
import alma.obops.draws.messages.AbstractRequestMessage;
import alma.obops.draws.messages.DbConnection;
import alma.obops.draws.messages.Executor;
import alma.obops.draws.messages.ExecutorClient;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.Publisher;
import alma.obops.draws.messages.RequestMessage;
import alma.obops.draws.messages.RequestProcessor;
import alma.obops.draws.messages.ResponseMessage;
import alma.obops.draws.messages.Subscriber;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.configuration.CouchDbConfiguration;
import alma.obops.draws.messages.configuration.CouchDbConfigurationProperties;
import alma.obops.draws.messages.configuration.CouchDbMessageBrokerConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { CouchDbConfiguration.class, 
							CouchDbConfigurationProperties.class, 
						    CouchDbMessageBrokerConfiguration.class
						    })
@ActiveProfiles( "couchdb" )
public class TestExecutor {

	private static final String QUEUE_NAME = "DOUBLER_Q";
	static Integer doubled = null; 			// needs to be static!
	
	@Autowired
	private MessageBroker broker = null;
	
	private Subscriber subscriber;
	private Publisher publisher;

	// Request: double a number
	public static class DoubleRequest extends AbstractRequestMessage  {
		public int number;

		public DoubleRequest() {
			// needed for JSON (de)serialization
		}
	}

	// Response: a doubled number
	public static class DoubleResponse extends AbstractMessage implements ResponseMessage {
		public int doubled;

		public DoubleResponse() {
			// needed for JSON (de)serialization
		}
	}

	// Doubles its input
	public static class Doubler implements RequestProcessor {

		@Override
		public ResponseMessage process( RequestMessage message ) {

			DoubleRequest request = (DoubleRequest) message;
			System.out.println( ">>> Received request with number: " + request.number );
			DoubleResponse response = new DoubleResponse();
			response.doubled = request.number + request.number;
			return response;
		}
	}
	
	@Before
	public void aaa_setUp() throws IOException {
		System.out.println( ">>> SETUP ========================================" );
		this.publisher = new Publisher( broker, QUEUE_NAME );
		this.subscriber = new Subscriber( broker, QUEUE_NAME, "test" );
		
		DbConnection db = ((CouchDbMessageBroker) broker).getDbConnection(); 
		db.dbDelete( MESSAGE_BUS_NAME );
		db.dbCreate( MESSAGE_BUS_NAME );
	}

	@Test
	public void testDoubler() throws Exception {

		// Start a background thread to run the Doubler Executor
		RequestProcessor doubler = new Doubler();
		Executor doublerExecutor = new Executor( this.subscriber, doubler, 5000 );
		Runnable doublerRunnable = () -> {	
			try {
				doublerExecutor.run();
			} 
			catch (TimeLimitExceededException e) {
				System.out.println(">>> Timed out: " + e.getMessage());
			} 
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		};
		Thread doublerThread = new Thread( doublerRunnable );
		doublerThread.start();
		
		// Define the client for that Executor
		MessageConsumer consumer = (message) -> {
			if( ! (message instanceof DoubleResponse) ) {
				String msg = "Not a " + DoubleResponse.class.getSimpleName() + ": " + message;
				
				System.out.println( ">>>>> message 2: " + message );
				System.out.println( ">>>>> Thread: " + Thread.currentThread().getName() );
				System.out.println( ">>>>> " + msg );
				throw new RuntimeException( msg );
			}
			doubled = ((DoubleResponse) message).doubled;
		};
		
		ExecutorClient client = new ExecutorClient( publisher, consumer );

		// Client sends a request to double 1
		DoubleRequest request = new DoubleRequest();
		doubled = null;
		request.number = 1;
		client.call( request );

//		MessageBroker.sleep( 1500 );	
		assertNotNull( doubled );
		System.out.println( ">>> Received reply with number: " + doubled );
		assertEquals( 2, doubled.intValue() );

		// Client sends a request to double 17
		doubled = null;
		request.number = 17;
		client.call( request );
		assertNotNull( doubled );
		System.out.println( ">>> Received reply with number: " + doubled );
		assertEquals( 34, doubled.intValue() );
		doublerThread.join();
	}
}
