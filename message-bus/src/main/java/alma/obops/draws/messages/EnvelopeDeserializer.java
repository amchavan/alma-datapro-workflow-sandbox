package alma.obops.draws.messages;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * We need a special Jackson deserializer for the {@link Envelope}, as the actual
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
	"creationTimestamp": "2018-09-12T14:21:19",
	"originIP": "134.171.73.110",
	"queueName": "xtss",
	"consumed": false
}
 * </pre>
 * 
 * From https://www.baeldung.com/jackson-nested-values
 * 
 * @author mchavan, 12-Sep-2018
 */

@SuppressWarnings("serial")
class EnvelopeDeserializer extends StdDeserializer<Envelope> {

	public EnvelopeDeserializer() {
        this( null );
    }
 
    public EnvelopeDeserializer(Class<?> vc) {
        super ( vc );
    }
    
    @Override
    public Envelope deserialize( JsonParser jp, DeserializationContext ctxt ) 
      throws IOException, JsonProcessingException {
  
        JsonNode node = jp.getCodec().readTree(jp);
        
        Envelope record = new Envelope();
        record.setId(                node.get( "_id" ).textValue() );
        record.setVersion(           node.get( "_rev" ).textValue() );
        record.setCreationTimestamp( node.get( "creationTimestamp" ).textValue() );
        record.setOriginIP(          node.get( "originIP" ).textValue() );
        record.setQueueName(         node.get( "queueName" ).textValue() );
        record.setConsumed(          node.get( "consumed" ).booleanValue() );
//        record.setMessage(           node.get( "message" ).toString() );

		String messageClass = node.get( "messageClass" ).textValue();
        try {
			if( messageClass != null ) {

				ObjectMapper objectMapper = new ObjectMapper();
				Class<?> clasz = Class.forName( messageClass );
				Message o = (Message) objectMapper.readValue( node.get( "message" ).toString(), clasz );
				record.setMessage( o );
			}
		} 
        catch( ClassNotFoundException e ) {
			String msg = "Deserialization of class '" + messageClass + "' failed: ";
			throw new IOException( msg, e );
		}
        
        return record;
    }
}