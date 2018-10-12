package alma.obops.draws.messages.rabbitmq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.SimpleEnvelope;
import alma.obops.draws.messages.TestUtils.TestMessage;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PersistenceConfiguration.class)
@AutoConfigureJdbc
public class TestPersistence {
	
	private static final String GROUP_NAME = "g";
	private static final String QUEUE_NAME = "Q";
	private static final String ORIGIN_IP = "0.0.0.0";
	private static final TestMessage freddie = new TestMessage( "Freddie Mercury", 45, false );
	private static final TestMessage    jimi = new TestMessage( "Jimi Hendrix", 28, false );

    @Autowired 
    RecipientGroupRepository recipientGroupRepository;
    
    @Autowired 
    PersistedEnvelopeRepository envelopeRepository;
    
	@Test
	public void envelope() {
		SimpleEnvelope envelope1 = new SimpleEnvelope( freddie, ORIGIN_IP, QUEUE_NAME, 0 );
		envelope1.setState( State.Sent );
		PersistedEnvelope pe = PersistedEnvelope.convert( envelope1 );
		PersistedEnvelope saved = envelopeRepository.save( pe );
		assertNotNull( saved.id );
		
		SimpleEnvelope envelope2 = new SimpleEnvelope( jimi, ORIGIN_IP, QUEUE_NAME, 0 );
		envelope2.setState( State.Received );
		PersistedEnvelope pe2 = PersistedEnvelope.convert( envelope2 );
		PersistedEnvelope saved2 = envelopeRepository.save( pe2 );
		assertNotNull( saved2.id );

		Optional<PersistedEnvelope> out1 = envelopeRepository.findByEnvelopeId( envelope1.getId() );
		assertTrue( out1.isPresent() );
		Optional<PersistedEnvelope> out2 = envelopeRepository.findByEnvelopeId( envelope2.getId() );
		assertTrue( out2.isPresent() );
		List<PersistedEnvelope> found1 = envelopeRepository.findByState( State.Sent.toString() );
		assertTrue( found1.size() == 1 );
		List<PersistedEnvelope> found2 = envelopeRepository.findByState( State.Received.toString() );
		assertTrue( found2.size() == 1 );

		Iterable<PersistedEnvelope> all = envelopeRepository.findAll();
		for (PersistedEnvelope p : all) {
			System.out.println( ">>> p: " + p );
		}
	}
	
    
	@Test
	public void recipientGroup() {
		RecipientGroup group = new RecipientGroup( GROUP_NAME );
		RecipientGroup saved = recipientGroupRepository.save( group );
		assertNotNull( saved.id );
		
		System.out.println( ">>> group: " + group );
		
		boolean added = saved.addMember( "m1" );
		assertTrue( added );
		added = saved.addMember( "m2" );
		assertTrue( added );
		added = saved.addMember( "m1" );
		assertFalse( added );
		
		RecipientGroup saved2 = recipientGroupRepository.save( saved );
		assertEquals( saved, saved2 );
		
		Optional<RecipientGroup> retrieved = recipientGroupRepository.findByName( GROUP_NAME );
		assertTrue( retrieved.isPresent() );
		assertEquals( saved, retrieved.get() );
	}
}
