package alma.obops.draws.messages.security;

import static alma.obops.draws.messages.TestUtils.RABBITMQ_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.TestUtils.TestMessage;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.rabbitmq.PersistedEnvelope;
import alma.obops.draws.messages.rabbitmq.PersistedEnvelopeRepository;
import alma.obops.draws.messages.rabbitmq.PersistenceConfiguration;
import alma.obops.draws.messages.rabbitmq.RabbitMqMessageBroker;
import alma.obops.draws.messages.rabbitmq.RecipientGroupRepository;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = PersistenceConfiguration.class)
@AutoConfigureJdbc
public class TestTokenSecurityRabbitMQ {

	private static final String QUEUE_NAME = "test.queue";
	private static final String EXCHANGE_NAME = "unit-test-exchange";
	
	private final TestMessage brian = new TestMessage( "Brian May", 71, true );
	private RabbitMqMessageBroker broker;
	private MessageQueue queue;
	private TokenFactory tokenFactory;
	
	@Autowired
	private PersistedEnvelopeRepository envelopeRepository;
	
	@Autowired
	private RecipientGroupRepository groupRepository;
	
	@Before
	public void aaa_setUp() throws Exception {

		System.out.println( ">>> SETUP ========================================" );
		
		this.broker = new RabbitMqMessageBroker( RABBITMQ_URL, EXCHANGE_NAME,
												 envelopeRepository, groupRepository );
		this.queue = broker.messageQueue( QUEUE_NAME );

		// Drain any existing messages in the logging queue
		broker.drainLoggingQueue();
		envelopeRepository.deleteAll();
		groupRepository.deleteAll();
		
		tokenFactory = MockedTokenFactory.getFactory();
		broker.setTokenFactory( tokenFactory );
	}

	@After
	public void aaa_tearDown() throws Exception {
		this.queue.delete();
	}
	
	@Test
	public void send_Secure_Receive() throws IOException, InterruptedException {

		MessageQueue queue = broker.messageQueue( QUEUE_NAME );

		queue.send( brian );
		Envelope out = queue.receive();
		
		assertNotNull( out );
		assertEquals( State.Received, out.getState() );
		assertNotNull( out.getReceivedTimestamp() );
		assertEquals( brian, out.getMessage() );
		assertEquals( out, out.getMessage().getEnvelope() );
		
		queue.delete();
	}
	
	@Test
	public void send_Secure_Reject() throws IOException, InterruptedException {

		// Give the broker a token that's been tampered with
		Map<String, Object> inProps = new HashMap<>();
		inProps.put( "valid", "false" );
		String token = tokenFactory.create( inProps );
		broker.setSendToken( token );
		
		MessageQueue queue = broker.messageQueue( QUEUE_NAME );
		queue.send( brian );
		

		Runnable messageLogListener = broker.getMessageLogListener();
		Thread messageLogThread = new Thread( messageLogListener );
		messageLogThread.start();
		
		try {
			// time out right away because we
			// should see that the message was 
			// rejected
			@SuppressWarnings("unused")
			Envelope out = queue.receive( 1000 );
		} 
		catch (TimeLimitExceededException e) {
			// no-op, expected
		}		

		messageLogThread.join();
		// Give some time to the background thread to catch up
		MessageBroker.sleep( 1000L );
		 
		Iterable<PersistedEnvelope> all = envelopeRepository.findAll();
		for (PersistedEnvelope p : all) {
			final State state = p.asSimpleEnvelope().getState();
			System.out.println( ">>> TestPersistence.envelope(): p: " + p + ", state: " + state );
			assertEquals( State.Rejected, state );
		}
		
		queue.delete();
	}
}
