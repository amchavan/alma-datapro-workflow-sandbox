package alma.icd.adapt.messagebus.rabbitmq;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import alma.icd.adapt.messagebus.Envelope;
import alma.icd.adapt.messagebus.Message;
import alma.icd.adapt.messagebus.SimpleEnvelope;
import alma.icd.adapt.messagebus.Envelope.State;

@Table( "envelope" )
public class PersistedEnvelope {

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
	public String getConsumedTimestamp() {
		return consumedTimestamp;
	}
	public String getEnvelopeId() {
		return envelopeId;
	}
	public String getExpiredTimestamp() {
		return expiredTimestamp;
	}
	public Long getId() {
		return id;
	} 
	public String getMessage() {
		return message;
	}
	public String getMessageClass() {
		return messageClass;
	}
	public String getOriginIp() {
		return originIp;
	}
	public String getQueueName() {
		return queueName;
	}
	public String getReceivedTimestamp() {
		return receivedTimestamp;
	}
	public String getRejectedTimestamp() {
		return rejectedTimestamp;
	}
	
	public String getSentTimestamp() {
		return sentTimestamp;
	}
	
	public String getState() {
		return state;
	}

	public Long getTimeToLive() {
		return timeToLive;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + envelopeId + "]";
	}
}
