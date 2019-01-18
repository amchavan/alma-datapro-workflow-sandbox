package alma.adapt.examples.dashboard.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author mchavan, 11-Jan-2019
 */

public abstract class Entity {
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	/**
	 * Entity version, mapped to the <code>_rev</code> property.
	 * 
	 * It's important that this property is initialized as <code>null</code> and
	 * doesn't get serialized if <code>null</code>, otherwise CouchDB will complain
	 * when creating a record for the first time.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonProperty("_rev")
	protected String version;

	@JsonInclude(Include.NON_NULL)
	@JsonProperty("_id")
	protected String id;

	public Entity() {
		this.version = null;
		this.id = null;
	}

	public Entity( String version, String id ) {
		this.version = version;
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public String getVersion() {
		return this.version;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}