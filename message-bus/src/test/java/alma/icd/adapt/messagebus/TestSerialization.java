package alma.icd.adapt.messagebus;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import alma.icd.adapt.messagebus.SimpleEnvelope;
import alma.icd.adapt.messagebus.TestUtils.TestMessage;
import alma.icd.adapt.messagebus.couchdb.CouchDbEnvelope;

public class TestSerialization {

	@Test
	public void testSimpleEnvelope() throws IOException, ClassNotFoundException {
		TestMessage jimi = new TestMessage( "Jimi Hendrix", 28, false );
		SimpleEnvelope in = new SimpleEnvelope( jimi, "134.171.1.1", "Q", 1000L );
		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString( in );
		System.out.println( ">>> " + json );
		
		SimpleEnvelope out = objectMapper.readValue( json, SimpleEnvelope.class );
		assertEquals( in, out );
	}
	
	@Test
	public void testCouchDbEnvelope() throws IOException, ClassNotFoundException {
		TestMessage jimi = new TestMessage( "Jimi Hendrix", 28, false );
		CouchDbEnvelope in = new CouchDbEnvelope( jimi, "134.171.1.1", "Q", 1000L );
		in.setVersion( "abcd" );
		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString( in );
		System.out.println( ">>> " + json );
		
		CouchDbEnvelope out = objectMapper.readValue( json, CouchDbEnvelope.class );
		assertEquals( in, out );
	}
}
