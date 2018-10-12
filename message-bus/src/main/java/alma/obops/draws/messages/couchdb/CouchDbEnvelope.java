package alma.obops.draws.messages.couchdb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.Record;
import alma.obops.draws.messages.SimpleEnvelope;

/**
 * Wrapper for user messages, adding message metadata like timestamp and ID.
 * 
 * @author mchavan, 12-Sep-2018
 */

@JsonDeserialize(using = CouchDbEnvelopeDeserializer.class)
public class CouchDbEnvelope extends SimpleEnvelope implements Record {
	/**
	 * Envelope version, mapped to the <code>_rev</code> property.
	 * 
	 * It's important that this property is initialized as <code>null</code> and
	 * doesn't get serialized if <code>null</code>, otherwise CouchDB will complain
	 * when creating a record for the first time.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonProperty("_rev")
	protected String version;

	public CouchDbEnvelope() {
		super();
		this.version = null;
	}

	public CouchDbEnvelope( Message message, String ourIP, String queueName, long timeToLive) {
		super( message, ourIP, queueName, timeToLive );
		this.version = null;
	}

	public CouchDbEnvelope( SimpleEnvelope envelope ) {
		super();
		this.id                = envelope.getId();
		this.message           = envelope.getMessage();
		this.messageClass      = envelope.getMessageClass();
		this.sentTimestamp     = envelope.getSentTimestamp();
		this.receivedTimestamp = envelope.getReceivedTimestamp();
		this.consumedTimestamp = envelope.getConsumedTimestamp();
		this.expiredTimestamp  = envelope.getExpiredTimestamp();
		this.originIP          = envelope.getOriginIP();
		this.queueName         = envelope.getQueueName();
		this.state             = envelope.getState();
		this.expireTime        = envelope.getExpireTime();
		this.version 	       = null;
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
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}	
	
	public String getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}