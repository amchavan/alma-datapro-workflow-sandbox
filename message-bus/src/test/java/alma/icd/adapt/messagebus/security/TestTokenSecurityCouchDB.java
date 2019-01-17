package alma.obops.draws.messages.security;

import static alma.obops.draws.messages.TestUtils.MESSAGE_BUS_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.text.ParseException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.nimbusds.jose.JOSEException;

import alma.obops.draws.messages.DbConnection;
import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.Publisher;
import alma.obops.draws.messages.Subscriber;
import alma.obops.draws.messages.TestUtils.TestMessage;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.configuration.CouchDbConfiguration;
import alma.obops.draws.messages.configuration.CouchDbConfigurationProperties;
import alma.obops.draws.messages.configuration.EmbeddedDataSourceConfiguration;
import alma.obops.draws.messages.configuration.PersistedEnvelopeRepository;
import alma.obops.draws.messages.configuration.PersistenceConfiguration;
import alma.obops.draws.messages.configuration.RecipientGroupRepository;
import alma.obops.draws.messages.couchdb.CouchDbConnection;
import alma.obops.draws.messages.couchdb.CouchDbMessageBroker;
import alma.obops.draws.messages.rabbitmq.PersistedEnvelope;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PersistenceConfiguration.class,
		 					CouchDbConfiguration.class, 
		 					CouchDbConfigurationProperties.class,
		 			        EmbeddedDataSourceConfiguration.class })
@ActiveProfiles( "unit-test-rabbitmq" )
@AutoConfigureJdbc
public class TestTokenSecurityCouchDB {


	private static final String QUEUE_NAME = "rock.stars";
	private static final String SERVICE_NAME = "local";
	private CouchDbMessageBroker broker = null;
	
	private final TestMessage brian = new TestMessage( "Brian May", 71, true );
	private DbConnection db;
	private TokenFactory tokenFactory;

	private Publisher publisher;
	private Subscriber subscriber;
	
	@Autowired
	private CouchDbConnection couchDbConn;
	
	@Autowired
	private PersistedEnvelopeRepository envelopeRepository;
	
	@Autowired
	private RecipientGroupRepository groupRepository;
	
	@Before
	public void aaa_setUp() throws IOException, ParseException, JOSEException {
		this.broker = new CouchDbMessageBroker( couchDbConn, MESSAGE_BUS_NAME  );
		this.publisher = new Publisher( broker, QUEUE_NAME );
		this.subscriber = new Subscriber( broker, QUEUE_NAME, SERVICE_NAME );
		this.db = ((CouchDbMessageBroker) broker).getDbConnection(); 
		this.db.dbDelete( MESSAGE_BUS_NAME );
		this.db.dbCreate( MESSAGE_BUS_NAME );
	
		this.envelopeRepository.deleteAll();
		this.groupRepository.deleteAll();
		
		this.tokenFactory = new JWTFactory();
		this.broker.setTokenFactory( tokenFactory );
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
		broker.setSendToken( token + "*****" );
		publisher.publish( brian );
		
		try {
			// time out right away because we
			// should see that the message was 
			// rejected
			@SuppressWarnings("unused")
			Envelope out = subscriber.receive( 1000 );
		} 
		catch (TimeLimitExceededException e) {
			// no-op, expected
		}		
		 
		Iterable<PersistedEnvelope> all = envelopeRepository.findAll();
		for (PersistedEnvelope p : all) {
			final State state = p.asSimpleEnvelope().getState();
			System.out.println( ">>> TestPersistence.envelope(): p: " + p + ", state: " + state );
			assertEquals( State.Rejected, state );
		}
	}
}
