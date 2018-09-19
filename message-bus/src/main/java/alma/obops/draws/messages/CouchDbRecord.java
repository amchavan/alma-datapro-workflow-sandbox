package alma.obops.draws.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A CouchDb record must have some ID and version string.
 * 
 * @author mchavan, 07-Sep-2018
 */
public abstract class CouchDbRecord {

	/**
	 * Record ID, mapped to the <code>_id</code> property.
	 */
	@JsonProperty("_id")
	protected String id;

	/**
	 * Record version, mapped to the <code>_rev</code> property.
	 * 
	 * It's important that this property is initialized as <code>null</code> and
	 * doesn't get serialized if <code>null</code>, otherwise CouchDB will complain
	 * when creating a record for the first time.
	 */
	@JsonInclude(Include.NON_NULL)
	@JsonProperty("_rev")
	protected String version;

	public CouchDbRecord() {
		this.id = null;
		this.version = null;
	}

	public CouchDbRecord( String id ) {
		this( id, null );
	}

	public CouchDbRecord( String id, String version ) {
		this.id = id;
		this.version = version;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CouchDbRecord other = (CouchDbRecord) obj;
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

	public String getId() {
		return this.id;
	}

	public String getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
