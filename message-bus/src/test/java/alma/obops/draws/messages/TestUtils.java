package alma.obops.draws.messages;

import alma.obops.draws.messages.couchdb.CouchDbRecord;

public class TestUtils {

	public static final String TEST_DB_NAME = "test-message-bus";
	public static final String MESSAGE_BUS_NAME = TEST_DB_NAME;
	public static final String COUCHDB_URL = "http://localhost:5984";
	public static final String COUCHDB_USERNAME = "admin";
	public static final String COUCHDB_PASSWORD = "admin";
	public static final String SELECTOR = "look.at.this";
	
	public static class TestRecord extends CouchDbRecord {
		static int nextID = 0;
		public String name;
		public int age;
		public boolean alive;
	
		public TestRecord() {
			super();
		}
		
		public TestRecord( String name, int age, boolean alive ) {
			this( String.valueOf( nextID++ ), name, age, alive );
		}
	
		public TestRecord( String id, String name, int age, boolean alive ) {
			this( id, null, name, age, alive );
		}
	
		public TestRecord( String id, String version, String name, int age, boolean alive ) {
			super( id, version );
			this.age = age;
			this.alive = alive;
			this.name = name;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestRecord other = (TestRecord) obj;
			if (age != other.age)
				return false;
			if (alive != other.alive)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + age;
			result = prime * result + (alive ? 1231 : 1237);
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		
		@Override
		public String toString() {
			return "TestRecord[name=" + name + ", age=" + age + ", alive=" + alive + ", id=" + id + ", version=" + version + "]";
		}
	}


	public static class TestMessage extends AbstractMessage {
		
		public String name;
		public int age;
		public boolean alive;
	
		public TestMessage() {
		}
		
		public TestMessage( String name, int age, boolean alive ) {
			this.age = age;
			this.alive = alive;
			this.name = name;
		}
	
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestMessage other = (TestMessage) obj;
			if (age != other.age)
				return false;
			if (alive != other.alive)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + age;
			result = prime * result + (alive ? 1231 : 1237);
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		
		@Override
		public String toString() {
			return "TestRecord[name=" + name + ", age=" + age + ", alive=" + alive + "]";
		}
	}
}
