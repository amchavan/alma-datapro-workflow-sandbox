package alma.obops.draws.messages.rabbitmq;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.SimpleEnvelope;

@Table( "envelope" )
public class PersistedEnvelope {

	/**
	 * Cannot use the original envelope ID as the ID for persistence when using
	 * Spring Data JDBC -- for an explanation see
	 * https://stackoverflow.com/questions/50371775/why-does-spring-data-jdbc-not-save-my-car-object
	 */
	@Id
	Long id;
	
	String envelopeId;
	String consumedTimestamp;
	String expiredTimestamp;
	String message;				// message gets converted to a JSON string before storing
	String messageClass;
	String originIp; 
	String queueName;
	String receivedTimestamp;
	String sentTimestamp;
	String rejectedTimestamp;
	String state;
	Long timeToLive;
	
	/** No-arg constructor needed by Spring Data */
	public PersistedEnvelope() {
		// empty
	}
	
	public static PersistedEnvelope convert( Envelope envelope ) {
		PersistedEnvelope ret = new PersistedEnvelope();
		ret.envelopeId         = envelope.getId();
		ret.consumedTimestamp  = envelope.getConsumedTimestamp();
		ret.expiredTimestamp   = envelope.getExpiredTimestamp();
		ret.messageClass       = envelope.getMessageClass();
		ret.originIp           = envelope.getOriginIP();
		ret.queueName          = envelope.getQueueName();
		ret.receivedTimestamp  = envelope.getReceivedTimestamp();
		ret.sentTimestamp      = envelope.getSentTimestamp();
		ret.state              = envelope.getState().toString();
		ret.timeToLive         = envelope.getTimeToLive();
		
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Message m = envelope.getMessage();
			String json = objectMapper.writeValueAsString( m );
			ret.message            = json;
		} 
		catch (JsonProcessingException e) {
			throw new RuntimeException( e );
		}
		
		return ret;
	}

	public SimpleEnvelope asSimpleEnvelope() {
		Message deserializedMessage = SimpleEnvelope.deserializeMessage(messageClass, message);
		SimpleEnvelope ret = new SimpleEnvelope( envelopeId, 
												 deserializedMessage, 
												 messageClass, 
												 sentTimestamp, 
												 receivedTimestamp, 
												 consumedTimestamp, 
												 expiredTimestamp, 
												 originIp, 
												 queueName, 
												 State.valueOf( state ), 
												 timeToLive );
		return ret;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + envelopeId + "]";
	}
}
