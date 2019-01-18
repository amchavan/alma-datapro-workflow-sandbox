package alma.icd.adapt.messagebus.couchdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * We need a special Jackson deserializer for the {@link ReceiverGroup}.<p>
 * From https://www.baeldung.com/jackson-nested-values
 * 
 * @author mchavan, 14-Sep-2018
 */

@SuppressWarnings("serial")
class ReceiverGroupDeserializer extends StdDeserializer<ReceiverGroup> {

	public ReceiverGroupDeserializer() {
        this( null );
    }
 
    public ReceiverGroupDeserializer(Class<?> vc) {
        super ( vc );
    }
    
	@SuppressWarnings("unchecked")
	@Override
    public ReceiverGroup deserialize( JsonParser jp, DeserializationContext ctxt ) 
      throws IOException, JsonProcessingException {
  
        JsonNode node = jp.getCodec().readTree(jp);
        
        ReceiverGroup group = new ReceiverGroup();
        group.setId(                node.get( "_id" ).textValue() );
        group.setVersion(           node.get( "_rev" ).textValue() );
        
		ObjectMapper objectMapper = new ObjectMapper();
		String members = node.get( "members" ).toString();
		List<String> o = new ArrayList<String>();
		o = (List<String>) objectMapper.readValue( members, o.getClass() );
		group.setMembers( o );
        
        return group;
    }
}