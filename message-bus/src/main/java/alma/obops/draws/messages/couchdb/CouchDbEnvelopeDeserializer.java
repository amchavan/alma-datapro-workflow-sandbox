package alma.obops.draws.messages.couchdb;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.Envelope.State;

/**
 * We need a special Jackson deserializer for the {@link CouchDbEnvelope}, as the actual
 * message is an embedded object of arbitrary type, for instance:
 * 
 * <pre>
{
	"_id": "abcd",
	"_rev": "1-3d9cae152c462e99e46fc988a135563b",
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
class CouchDbEnvelopeDeserializer extends StdDeserializer<CouchDbEnvelope> {

	public CouchDbEnvelopeDeserializer() {
        this( null );
    }
 
    public CouchDbEnvelopeDeserializer(Class<?> vc) {
        super ( vc );
    }
    
    @Override
    public CouchDbEnvelope deserialize( JsonParser jp, DeserializationContext ctxt ) 
      throws IOException, JsonProcessingException {
  
        JsonNode node = jp.getCodec().readTree(jp);
        
        CouchDbEnvelope envelope = new CouchDbEnvelope();
        JsonNode etNode = node.get( "expireTime" );
        
		envelope.setExpireTime(        etNode.isNull() ? null : etNode.longValue() );
        envelope.setId(                node.get( "_id" ).textValue() );
        envelope.setVersion(           node.get( "_rev" ).textValue() );
        envelope.setSentTimestamp(     node.get( "sentTimestamp" ).textValue() );
        envelope.setReceivedTimestamp( node.get( "receivedTimestamp" ).textValue() );
        envelope.setConsumedTimestamp( node.get( "consumedTimestamp" ).textValue() );
        envelope.setExpiredTimestamp(  node.get( "expiredTimestamp" ).textValue() );
        envelope.setOriginIP(          node.get( "originIP" ).textValue() );
        envelope.setQueueName(         node.get( "queueName" ).textValue() );
        envelope.setState( State.valueOf( node.get( "state" ).textValue() ));
		String messageClass = node.get( "messageClass" ).textValue();
        envelope.setMessageClass(       messageClass );
        
        try {
			if( messageClass != null ) {

				ObjectMapper objectMapper = new ObjectMapper();
				Class<?> clasz = Class.forName( messageClass );
				Message o = (Message) objectMapper.readValue( node.get( "message" ).toString(), clasz );
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