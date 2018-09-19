package alma.obops.draws.messages;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Defines a group of receivers, analogous to a WhatsApp group
 * @author mchavan, 12-Sep-2018
 */

@JsonDeserialize(using = ReceiverGroupDeserializer.class)
public class ReceiverGroup extends CouchDbRecord {
	List<String> members;

	public ReceiverGroup() {
		this.members = new ArrayList<String>();
	}

	public ReceiverGroup( String groupName ) {
		super( groupName );
		this.members = new ArrayList<String>();
	}
	
	public void add( String member ) {
		members.add( member );
	}

	public List<String> getMembers() {
		return members;
	}
	
	public void remove( String member ) {
		members.remove( member );
	}
	
	public void setMembers(List<String> members) {
		this.members = members;
	}
}
