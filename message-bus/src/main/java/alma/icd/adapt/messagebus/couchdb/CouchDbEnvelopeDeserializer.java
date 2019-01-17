package alma.obops.draws.messages.couchdb;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.SimpleEnvelope;

/**
 * We need a special Jackson deserializer for the {@link SimpleEnvelope}, as the
 * contained message is an embedded object of arbitrary type, for instance:
 * 
 * <pre>
{
	"_id": "abcd",
	"message": {
		...
	},
	"messageClass": "alma.obops.draws.messages....",
	"sentTimestamp": "2018-09-12T14:21:19",
	"originIP": "134.171.73.110",
	"queueName": "xtss",
	...
}
 * </pre>
 * 
 * From https://www.baeldung.com/jackson-nested-values
 * 
 * @author mchavan, 12-Sep-2018
 */

@SuppressWarnings("serial")
public class CouchDbEnvelopeDeserializer extends StdDeserializer<CouchDbEnvelope> {

	public CouchDbEnvelopeDeserializer() {
        this( null );
    }
 
    public CouchDbEnvelopeDeserializer(Class<?> vc) {
        super ( vc );
    }
    
    @Override
    public CouchDbEnvelope deserialize( JsonParser jp, DeserializationContext ctxt ) 
      throws IOException, JsonProcessingException {

        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        ObjectNode root = (ObjectNode) mapper.readTree(jp);
        
        CouchDbEnvelope envelope = new CouchDbEnvelope();
        
        JsonNode etNode = root.get( "expireTime" );
		Long expireTime = (etNode == null || etNode.isNull()) ? null : etNode.longValue();

		String messageClass = root.get( "messageClass" ).textValue();
        State state = State.valueOf( root.get( "state" ).textValue() );
    	
        JsonNode revNode = root.get( "_rev" );
		String revision = (revNode == null || revNode.isNull()) ? null : revNode.textValue();
		
		envelope.setExpireTime(        expireTime );
        envelope.setId(                root.get( "_id" ).textValue() );
        envelope.setVersion(           revision );
		envelope.setSentTimestamp(     root.get( "sentTimestamp" ).textValue() );
        envelope.setReceivedTimestamp( root.get( "receivedTimestamp" ).textValue() );
        envelope.setConsumedTimestamp( root.get( "consumedTimestamp" ).textValue() );
        envelope.setExpiredTimestamp(  root.get( "expiredTimestamp" ).textValue() );
        envelope.setRejectedTimestamp( root.get( "rejectedTimestamp" ).textValue() );
        envelope.setOriginIP(          root.get( "originIP" ).textValue() );
        envelope.setQueueName(         root.get( "queueName" ).textValue() );
        envelope.setToken(      	   root.get( "token" ).textValue() );
		envelope.setState( 			   state );
        envelope.setMessageClass(      messageClass );
        
        try {
			if( messageClass != null ) {

				ObjectMapper objectMapper = new ObjectMapper();
				Class<?> clasz = Class.forName( messageClass );
				Message o = (Message) objectMapper.readValue( root.get( "message" ).toString(), clasz );
				envelope.setMessage( o );
				
				// Need to set envelope manually -- Jackson cannot
				// cope with circular references like Envelope -> Message -> Envelope
				o.setEnvelope( envelope );	 
			}
		} 
        catch( ClassNotFoundException e ) {
			String msg = "Deserialization of class '" + messageClass + "' failed: ";
			throw new IOException( msg, e );
		}
		
        return envelope;
    }
}