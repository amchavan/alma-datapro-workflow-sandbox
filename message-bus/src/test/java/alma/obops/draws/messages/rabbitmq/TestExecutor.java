package alma.obops.draws.messages.rabbitmq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import alma.obops.draws.messages.AbstractMessage;
import alma.obops.draws.messages.AbstractRequestMessage;
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
import alma.obops.draws.messages.configuration.EmbeddedDataSourceConfiguration;
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
public class TestExecutor {

	private static final String QUEUE_NAME = "test.executor.queue";
//	private static final String EXCHANGE_NAME = "unit-test-exchange";
	static Integer doubled = null; 			// needs to be static!
	
	@Autowired
	private MessageBroker broker;
	
	private RabbitMqMessageBroker rmqBroker = null;
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
	public void aaa_setUp() throws IOException, TimeoutException {
		System.out.println( ">>> SETUP ========================================" );
		this.publisher = new Publisher( broker, QUEUE_NAME );
		this.subscriber = new Subscriber( broker, QUEUE_NAME, "test" );

		this.rmqBroker = (RabbitMqMessageBroker) this.broker;
		this.rmqBroker.drainLoggingQueue();
		this.rmqBroker.drainQueue( this.subscriber.getQueue() );
	}

	@After
	public void aaa_tearDown() throws Exception {
		broker.deleteQueue( this.subscriber.getQueue() );
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
