package alma.adapt.examples.dashboard.domain;

import java.util.Map;

/**
 * @author mchavan, 11-Jan-2019
 */

public class OusStatus extends Entity {

	protected String entityId;
	protected String progId;
	protected String state;
	protected String substate;
	protected Map<String,String> flags;
	protected String timestamp;
	
	public OusStatus() {
		super();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OusStatus other = (OusStatus) obj;
		if (entityId == null) {
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (flags == null) {
			if (other.flags != null)
				return false;
		} else if (!flags.equals(other.flags))
			return false;
		if (progId == null) {
			if (other.progId != null)
				return false;
		} else if (!progId.equals(other.progId))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (substate == null) {
			if (other.substate != null)
				return false;
		} else if (!substate.equals(other.substate))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		return true;
	}

	public OusStatus( String entityId, String progId, String state, String substate, Map<String,String> flags, String timestamp) {
		super( entityId.replace( '/', '_' ).replace( ':', '_' ), null );
		this.entityId = entityId;
		this.progId = progId;
		this.state = state;
		this.substate = substate;
		this.flags = flags;
		this.timestamp = timestamp;
	}

	public String getEntityId() {
		return entityId;
	}

	public Map<String,String> getFlags() {
		return flags;
	}

	public String getProgId() {
		return progId;
	}

	public String getState() {
		return state;
	}

	public String getSubstate() {
		return substate;
	}

	public String getTimestamp() {
		return timestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result + ((flags == null) ? 0 : flags.hashCode());
		result = prime * result + ((progId == null) ? 0 : progId.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((substate == null) ? 0 : substate.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		return result;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public void setFlags( Map<String,String> flags ) {
		this.flags = flags;
	}

	public void setProgId(String progId) {
		this.progId = progId;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setSubstate(String substate) {
		this.substate = substate;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
}