package alma.obops.draws.messages.couchdb;

import java.util.UUID;

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
	private String creationTimestamp;
	private String originIP;
	private String queueName;
	private boolean consumed;
	
	public CouchDbEnvelope() {
		super();
	}
	
	public CouchDbEnvelope( Message message, String creationTimestamp, String originIP,
						    String queueName ) {
		super( makeID(), null );
		
		this.message = message;
		this.messageClass = message != null ? message.getClass().getName() : null;
		this.creationTimestamp = creationTimestamp;
		this.originIP = originIP;
		this.queueName = queueName;
		this.consumed = false;
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
		if (consumed != other.consumed)
			return false;
		if (creationTimestamp == null) {
			if (other.creationTimestamp != null)
				return false;
		} else if (!creationTimestamp.equals(other.creationTimestamp))
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
		return true;
	}
	
	public String getCreationTimestamp() {
		return creationTimestamp;
	}

	public Message getMessage() {
		return message;
	}
	
	public String getMessageClass() {
		return messageClass;
	}
	
	public String getOriginIP() {
		return originIP;
	}

	public String getQueueName() {
		return queueName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (consumed ? 1231 : 1237);
		result = prime * result + ((creationTimestamp == null) ? 0 : creationTimestamp.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((messageClass == null) ? 0 : messageClass.hashCode());
		result = prime * result + ((originIP == null) ? 0 : originIP.hashCode());
		result = prime * result + ((queueName == null) ? 0 : queueName.hashCode());
		return result;
	}

	public boolean isConsumed() {
		return consumed;
	}

	public void setConsumed(boolean consumed) {
		this.consumed = consumed;
	}

	public void setCreationTimestamp(String creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
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

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ "[message=" + message + ", creationTimestamp=" + creationTimestamp + ", originIP="
				+ originIP + ", queueName=" + queueName + ", consumed=" + consumed 
				+ "]";
	}
	
	/**
	 * Compare by creation timestamp
	 */
	@Override
	public int compareTo( CouchDbEnvelope other ) {
		CouchDbEnvelope otherMessage = (CouchDbEnvelope) other;
		return this.getCreationTimestamp().compareTo( otherMessage.getCreationTimestamp() );
	}
}