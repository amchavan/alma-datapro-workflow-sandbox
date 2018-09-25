package alma.obops.draws.messages.couchdb;

import static alma.obops.draws.messages.MessageBus.now;
import static alma.obops.draws.messages.MessageBus.parseIsoDatetime;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.MessageBus;

/**
 * Wrapper for user messages, adding message metadata like timestamp and ID.
 * 
 * @author mchavan, 12-Sep-2018
 */
@JsonDeserialize(using = CouchDbEnvelopeDeserializer.class)
public class CouchDbEnvelope extends CouchDbRecord implements Envelope, Comparable<CouchDbEnvelope> {

	static String makeID() {
		StringBuilder sb = new StringBuilder();
		sb.append( MessageBus.nowISO() )
		  .append( "-" )
		  .append( UUID.randomUUID().toString().replace( "-", "" ) );
		
		return sb.toString();
	}

	private Message message;
	private String messageClass;
	private String sentTimestamp;
	private String receivedTimestamp;
	private String consumedTimestamp;
	private String expiredTimestamp;
	private String originIP;
	private String queueName;
	private State state;
	private Long expireTime;
	
	public CouchDbEnvelope() {
		super();
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
	public CouchDbEnvelope( Message message, String originIP,
						    String queueName, Long expireTime ) {
		super( makeID(), null );
		
		this.message = message;
		this.messageClass = message != null ? message.getClass().getName() : null;
		this.originIP = originIP;
		this.queueName = queueName;
		this.expireTime = expireTime;
	}
	
	/**
	 * Compare by creation timestamp
	 */
	@Override
	public int compareTo( CouchDbEnvelope other ) {
		CouchDbEnvelope otherMessage = (CouchDbEnvelope) other;
		return this.getSentTimestamp().compareTo( otherMessage.getSentTimestamp() );
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CouchDbEnvelope other = (CouchDbEnvelope) obj;
		if (consumedTimestamp == null) {
			if (other.consumedTimestamp != null)
				return false;
		} else if (!consumedTimestamp.equals(other.consumedTimestamp))
			return false;
		if (expiredTimestamp == null) {
			if (other.expiredTimestamp != null)
				return false;
		} else if (!expiredTimestamp.equals(other.expiredTimestamp))
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
		if (expireTime == null) {
			if (other.expireTime != null)
				return false;
		} else if (!expireTime.equals(other.expireTime))
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
		if( (expireTime == null) || (this.getState() != State.Sent) ) {
			// YES, not expiring
			return -1;
		}
		
		try {
			Date sent = parseIsoDatetime( sentTimestamp );
			Date now = now();
			long timeLived = now.getTime() - sent.getTime();
			long remainingTimeToLive = expireTime.longValue() - timeLived;
			
			System.out.println( ">>> Envelope: " + this );
			System.out.println( ">>>     now: " + now );
			System.out.println( ">>>     timeToLive: " + expireTime.longValue() );
			System.out.println( ">>>     timeLived: " + timeLived );
			System.out.println( ">>>     remainingTimeToLive: " + remainingTimeToLive );
			return remainingTimeToLive <= 0 ? 0 : remainingTimeToLive;
		}
		catch( ParseException e ) {
			// TODO Improve logging of this
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((consumedTimestamp == null) ? 0 : consumedTimestamp.hashCode());
		result = prime * result + ((expiredTimestamp == null) ? 0 : expiredTimestamp.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((messageClass == null) ? 0 : messageClass.hashCode());
		result = prime * result + ((originIP == null) ? 0 : originIP.hashCode());
		result = prime * result + ((queueName == null) ? 0 : queueName.hashCode());
		result = prime * result + ((receivedTimestamp == null) ? 0 : receivedTimestamp.hashCode());
		result = prime * result + ((sentTimestamp == null) ? 0 : sentTimestamp.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((expireTime == null) ? 0 : expireTime.hashCode());
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

	public void setSentTimestamp( String sentTimestamp  ) {
		this.sentTimestamp = sentTimestamp;
	}

	public void setState( State state ) {
		this.state = state;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ "[message=" + message + ", sent=" + sentTimestamp + ", originIP="
				+ originIP + ", queueName=" + queueName + ", state=" + state 
				+ "]";
	}
}