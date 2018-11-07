package alma.obops.draws.messages.rabbitmq;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

/** 
 * Super easy, brute force implementation
 * @author mchavan, 10-Oct-2017
 */
public class RecipientGroup {

	private static final String MEMBER_SEPARATOR = ",";

	@Id
	Long id;
	
	String groupName;
	String groupMembers;	// comma-separated list of recipients
	
	/** No-arg constructor needed by Spring Data */
	public RecipientGroup() {
		this.groupName = null;
		this.groupMembers = null;
	}
	
	public RecipientGroup( String groupName ) {
		this();
		this.groupName = groupName;
	}

	/**
	 * @return <code>true</code> if newMember was added to the group, <code>false</code> otherwise;
	 */
	public boolean addMember( String newMember ) {
		
		if( groupMembers == null ) {
			groupMembers = newMember;
			return true;
		}
		
		String[] members = groupMembers.split( MEMBER_SEPARATOR );
		
		// Do we already have that new member?
		for (int i = 0; i < members.length; i++) {
			if( members[i].equals( newMember )) {
				return false;					// YES, nothing to do
			}
		}
		
		// NO, need to add the new member
		if( members.length > 0 ) {
			// if needed, add a separator
			groupMembers += MEMBER_SEPARATOR;
		}
		groupMembers += newMember;
		return true;		
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupMembers == null) ? 0 : groupMembers.hashCode());
		result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
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
		RecipientGroup other = (RecipientGroup) obj;
		if (groupMembers == null) {
			if (other.groupMembers != null)
				return false;
		} else if (!groupMembers.equals(other.groupMembers))
			return false;
		if (groupName == null) {
			if (other.groupName != null)
				return false;
		} else if (!groupName.equals(other.groupName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + groupName + ": " + groupMembers + "]";
	}

	public List<String> getGroupMembersAsList() {
		List<String> ret = new ArrayList<>();
		String[] members = groupMembers.split( MEMBER_SEPARATOR );

		for (int i = 0; i < members.length; i++) {
			ret.add( members[i] );
		}
		return ret;
	}
}
