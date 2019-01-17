package alma.obops.draws.messages;

import static alma.obops.draws.messages.MessageBroker.now;
import static alma.obops.draws.messages.MessageBroker.parseIsoDatetime;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Wrapper for user messages, adding message metadata like timestamp and ID.
 * 
 * @author mchavan, 12-Sep-2018
 */

@JsonDeserialize(using = SimpleEnvelopeDeserializer.class)
public class SimpleEnvelope implements Envelope, Comparable<SimpleEnvelope> {

	/**
	 * Convert a JSON string to a {@link SimpleEnvelope}, allowing for subclasses to
	 * redefine this
	 */
	@SuppressWarnings("unchecked")
	public static SimpleEnvelope deserialize( String json ) {

		try {
			// First we convert the JSON string to a map of name/value pairs
			ObjectMapper objectMapper = new ObjectMapper();
			HashMap<String,Object> envelopeFields = new HashMap<String,Object>();
			envelopeFields = objectMapper.readValue( json, envelopeFields.getClass() );
			
			Map<String,Object> messageFields = (Map<String, Object>) envelopeFields.get( "message" );
			String serializedMessage = objectMapper.writeValueAsString( messageFields );

			String messageClassName = envelopeFields.get( "messageClass" ).toString();
			Message message = deserializeMessage( messageClassName, serializedMessage );
			
			// Convert the envelope without the contained message 
			envelopeFields.remove( "message" );
			String serializedEnvelope = objectMapper.writeValueAsString( envelopeFields );
			SimpleEnvelope envelope = objectMapper.readValue( serializedEnvelope, SimpleEnvelope.class );

			// Re-establish the connections between envelope and contained message
			envelope.setMessage( message );
			message.setEnvelope( envelope );
			return envelope;
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	/** 
	 * Convert a JSON string to a message
	 */
	public static Message deserializeMessage( String messageClassName, String serializedMessage ) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Class<?> messageClass = Class.forName( messageClassName );
			Message message = (Message) objectMapper.readValue( serializedMessage, messageClass );
			return message;
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	protected static String makeID() {
		StringBuilder sb = new StringBuilder();
		sb.append( MessageBroker.nowISO() )
		  .append( "-" )
		  .append( UUID.randomUUID().toString().replace( "-", "" ) );
		
		return sb.toString();
	}

	
	/**
	 * Envelope ID, mapped to the <code>_id</code> property.
	 */
	@JsonProperty("_id")
	protected String id;
	
	protected Message message;
	protected String messageClass;
	protected String originIP;
	protected String queueName;
	
	protected String sentTimestamp;
	protected String receivedTimestamp;
	protected String consumedTimestamp;
	protected String expiredTimestamp;
	protected String rejectedTimestamp;


	protected State state;
	protected long expireTime;
	protected String token;
	public SimpleEnvelope() {
		super();
		this.state = State.Sent;
	}
	/**
	 * @param message
	 *            The {@linkplain Message} we enclose
	 * @param originIP
	 *            The IP address of the host where this instance was generated
	 * @param queueName
	 *            Name of the queue to which this message should be sent
	 * @param expireTime
	 *            Time in msec before the enclosed message expires, if it's not read.
	 *            If <code>null</code, the message never expires
	 */
	public SimpleEnvelope( Message message, String originIP,
						   String queueName, long expireTime ) {
		this();
		this.id = makeID();
		this.message = message;
		this.message.setEnvelope( this );
		this.messageClass = message != null ? message.getClass().getName() : null;
		this.originIP = originIP;
		this.queueName = queueName;
		this.expireTime = expireTime;
	}
	
	/** All-fields constructor */
	public SimpleEnvelope( String id, Message message, String messageClass, String sentTimestamp,
			String receivedTimestamp, String consumedTimestamp, String expiredTimestamp, String originIP,
			String queueName, State state, long expireTime) {
		this.id = id;
		this.message = message;
		this.messageClass = messageClass;
		this.sentTimestamp = sentTimestamp;
		this.receivedTimestamp = receivedTimestamp;
		this.consumedTimestamp = consumedTimestamp;
		this.expiredTimestamp = expiredTimestamp;
		this.originIP = originIP;
		this.queueName = queueName;
		this.state = state;
		this.expireTime = expireTime;
	}

	/**
	 * Compare by creation timestamp
	 */
	@Override
	public int compareTo( SimpleEnvelope other ) {
		SimpleEnvelope otherMessage = (SimpleEnvelope) other;
		return this.getSentTimestamp().compareTo( otherMessage.getSentTimestamp() );
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleEnvelope other = (SimpleEnvelope) obj;
		if (consumedTimestamp == null) {
			if (other.consumedTimestamp != null)
				return false;
		} else if (!consumedTimestamp.equals(other.consumedTimestamp))
			return false;
		if (expireTime != other.expireTime)
			return false;
		if (expiredTimestamp == null) {
			if (other.expiredTimestamp != null)
				return false;
		} else if (!expiredTimestamp.equals(other.expiredTimestamp))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (messageClass == null) {
			if (other.messageClass != null)
				return false;
		} else if (!messageClass.equals(other.messageClass))
			return false;
		if (originIP == null) {
			if (other.originIP != null)
				return false;
		} else if (!originIP.equals(other.originIP))
			return false;
		if (queueName == null) {
			if (other.queueName != null)
				return false;
		} else if (!queueName.equals(other.queueName))
			return false;
		if (receivedTimestamp == null) {
			if (other.receivedTimestamp != null)
				return false;
		} else if (!receivedTimestamp.equals(other.receivedTimestamp))
			return false;
		if (sentTimestamp == null) {
			if (other.sentTimestamp != null)
				return false;
		} else if (!sentTimestamp.equals(other.sentTimestamp))
			return false;
		if (state != other.state)
			return false;
		return true;
	}

	@Override
	public String getConsumedTimestamp() {
		return this.consumedTimestamp;
	}


	@Override
	public String getExpiredTimestamp() {
		return expiredTimestamp;
	}
	
	public Long getExpireTime() {
		return expireTime;
	}
	
	public String getId() {
		return id;
	}
	
	@Override
	public Message getMessage() {
		return message;
	}

	@Override
	public String getMessageClass() {
		return messageClass;
	}
	
	@Override
	public String getOriginIP() {
		return originIP;
	}

	@Override
	public String getQueueName() {
		return queueName;
	}
	
	@Override
	public String getReceivedTimestamp() {
		return this.receivedTimestamp;
	}
	
	public String getRejectedTimestamp() {
		return rejectedTimestamp;
	}

	@Override
	public String getSentTimestamp() {
		return sentTimestamp;
	}

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	@JsonIgnore
	public long getTimeToLive() {
		
		// Was this message read at some point in the past?
		// But wait: does it even expire at all?
		if( (expireTime == 0) || (this.getState() != State.Sent) ) {
			// YES, not expiring
			return -1;
		}
		
		// Should never happen, except maybe in tests
		if( sentTimestamp == null ) {
			return -1;
		}
		
		try {
			Date sent = parseIsoDatetime( sentTimestamp );
			Date now = now();
			long timeLived = now.getTime() - sent.getTime();
			long remainingTimeToLive = expireTime - timeLived;
			
			// System.out.println( ">>> Envelope: " + this );
			// System.out.println( ">>>     now: " + now );
			// System.out.println( ">>>     timeToLive: " + expireTime );
			// System.out.println( ">>>     timeLived: " + timeLived );
			// System.out.println( ">>>     remainingTimeToLive: " + remainingTimeToLive );
			return remainingTimeToLive <= 0 ? 0 : remainingTimeToLive;
		}
		catch( ParseException e ) {
			// TODO Improve logging of this
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	@Override
	public String getToken() {
		return token;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consumedTimestamp == null) ? 0 : consumedTimestamp.hashCode());
		result = prime * result + (int) (expireTime ^ (expireTime >>> 32));
		result = prime * result + ((expiredTimestamp == null) ? 0 : expiredTimestamp.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((messageClass == null) ? 0 : messageClass.hashCode());
		result = prime * result + ((originIP == null) ? 0 : originIP.hashCode());
		result = prime * result + ((queueName == null) ? 0 : queueName.hashCode());
		result = prime * result + ((receivedTimestamp == null) ? 0 : receivedTimestamp.hashCode());
		result = prime * result + ((sentTimestamp == null) ? 0 : sentTimestamp.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	public void setConsumedTimestamp( String consumedTimestamp  ) {
		this.consumedTimestamp = consumedTimestamp;
	}

	public void setExpiredTimestamp( String expiredTimestamp ) {
		this.expiredTimestamp = expiredTimestamp;
	}

	public void setExpireTime( Long expireTime  ) {
		this.expireTime = expireTime;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public void setMessage( Message message) {
		this.message = message;
	}

	public void setMessageClass(String messageClass) {
		this.messageClass = messageClass;
	}

	public void setOriginIP(String originIP) {
		this.originIP = originIP;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public void setReceivedTimestamp( String receivedTimestamp  ) {
		this.receivedTimestamp = receivedTimestamp;
	}

	public void setRejectedTimestamp(String rejectedTimestamp) {
		this.rejectedTimestamp = rejectedTimestamp;
	}

	public void setSentTimestamp( String sentTimestamp  ) {
		this.sentTimestamp = sentTimestamp;
	}

	public void setState( State state ) {
		this.state = state;
	}

	public void setToken( String token ) {
		this.token = token;
	}

	@Override
	public String toString() {
		
		String msg = this.getClass().getSimpleName()
				+ "[message=" + message + ", sent=" + sentTimestamp + ", originIP="
				+ originIP + ", queueName=" + queueName + ", state=" + state;
		if( token != null ) {
			msg += ", token=" + token.substring( 0, 10 ) + "...";
		}
		msg += "]";
		return msg;
	}
}