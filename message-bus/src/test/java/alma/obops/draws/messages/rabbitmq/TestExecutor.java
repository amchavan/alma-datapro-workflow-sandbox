package alma.obops.draws.messages.rabbitmq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import alma.obops.draws.messages.AbstractMessage;
import alma.obops.draws.messages.Executor;
import alma.obops.draws.messages.ExecutorClient;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.RequestMessage;
import alma.obops.draws.messages.RequestProcessor;
import alma.obops.draws.messages.ResponseMessage;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.configuration.PersistedEnvelopeRepository;
import alma.obops.draws.messages.configuration.RecipientGroupRepository;

public class TestExecutor {

	private static final String QUEUE_NAME = "test.executor.queue";
	private static final String EXCHANGE_NAME = "unit-test-exchange";
	static Integer doubled = null; 			// needs to be static!
	private RabbitMqMessageBroker broker = null;
	private MessageQueue queue;

	@Autowired
	private PersistedEnvelopeRepository envelopeRepository;
	
	@Autowired
	private RecipientGroupRepository groupRepository;

	// Request: double a number
	public static class DoubleRequest extends AbstractMessage implements RequestMessage  {
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
			System.out.println( ">>> Received request with number=" + request.number );
			DoubleResponse response = new DoubleResponse();
			response.doubled = request.number + request.number;
			return response;
		}
	}
	
	@Before
	public void aaa_setUp() throws IOException, TimeoutException {
		System.out.println( ">>> SETUP ========================================" );
		this.broker = new RabbitMqMessageBroker( "amqp://localhost:5672",
												 "guest",
												 "guest",
												 EXCHANGE_NAME,
												 envelopeRepository, 
												 groupRepository );
		this.queue = broker.messageQueue( QUEUE_NAME );

		broker.drainLoggingQueue();
		broker.drainQueue( this.queue );
	}

	@After
	public void aaa_tearDown() throws Exception {
		broker.deleteQueue( this.queue );
	}

	@Test
	public void testDoubler() throws Exception {

		// Start a background thread to run the Doubler Executor
		RequestProcessor doubler = new Doubler();
		Executor doublerExecutor = new Executor( queue, doubler, 5000 );
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
		
		ExecutorClient client = new ExecutorClient( queue, consumer );

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
