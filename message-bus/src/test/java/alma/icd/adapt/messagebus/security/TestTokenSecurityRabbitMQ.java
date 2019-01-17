package alma.obops.draws.messages.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.Publisher;
import alma.obops.draws.messages.Subscriber;
import alma.obops.draws.messages.TestUtils.TestMessage;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.configuration.EmbeddedDataSourceConfiguration;
import alma.obops.draws.messages.configuration.PersistedEnvelopeRepository;
import alma.obops.draws.messages.configuration.PersistedRabbitMqBrokerConfiguration;
import alma.obops.draws.messages.configuration.PersistenceConfiguration;
import alma.obops.draws.messages.configuration.RabbitMqConfigurationProperties;
import alma.obops.draws.messages.rabbitmq.PersistedEnvelope;
import alma.obops.draws.messages.rabbitmq.RabbitMqMessageBroker;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PersistenceConfiguration.class,
        PersistedRabbitMqBrokerConfiguration.class,
        RabbitMqConfigurationProperties.class,
        EmbeddedDataSourceConfiguration.class } )
@ActiveProfiles( "unit-test-rabbitmq" )
@AutoConfigureJdbc
public class TestTokenSecurityRabbitMQ {

	private static final String QUEUE_NAME = "rock.stars";
	private static final String SERVICE_NAME = "local";
	
	private final TestMessage brian = new TestMessage( "Brian May", 71, true );
	
	@Autowired
	private MessageBroker broker;
	
	private Publisher publisher;
	private Subscriber subscriber;
	private TokenFactory tokenFactory;
	
	@Autowired
	private PersistedEnvelopeRepository envelopeRepository;
	private RabbitMqMessageBroker rmqBroker;
	
	@Before
	public void aaa_setUp() throws Exception {

//		System.out.println( ">>> SETUP ========================================" );

		this.publisher = new Publisher( broker, QUEUE_NAME );
		this.subscriber = new Subscriber( broker, QUEUE_NAME, SERVICE_NAME );

		this.rmqBroker = (RabbitMqMessageBroker) broker;
		this.rmqBroker.drainLoggingQueue();
		this.rmqBroker.getEnvelopeRepository().deleteAll();
		
		tokenFactory = new JWTFactory();
		broker.setTokenFactory( tokenFactory );
	}

	@After
	public void aaa_tearDown() throws Exception {
		this.subscriber.getQueue().delete();
	}
	
	@Test
	public void send_Secure_Receive() throws IOException, InterruptedException {

		publisher.publish( brian );
		Envelope out = subscriber.receive();
		
		assertNotNull( out );
		assertEquals( State.Received, out.getState() );
		assertNotNull( out.getReceivedTimestamp() );
		assertEquals( brian, out.getMessage() );
		assertEquals( out, out.getMessage().getEnvelope() );
	}
	
	@Test
	public void send_Secure_Reject() throws IOException, InterruptedException {

		// Give the broker a token that's been tampered with
		String token = tokenFactory.create();
		int l = token.length();
		rmqBroker.setSendToken( token.substring( 0, l-2 ));
		@SuppressWarnings("unused")
		Envelope e = publisher.publish( brian );
		

		Runnable messageLogListener = rmqBroker.getMessageArchiver();
		Thread messageLogThread = new Thread( messageLogListener );
		messageLogThread.start();
		
		try {
			// time out right away because we
			// should see that the message was 
			// rejected
			@SuppressWarnings("unused")
			Envelope out = subscriber.receive( 1000 );
		} 
		catch (TimeLimitExceededException ex) {
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
	}
}
