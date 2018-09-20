package alma.obops.draws.messages;

import static alma.obops.draws.messages.MessageBus.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CouchDbMessageBus implements MessageBus {
	    
	private String busName;
	private CouchDBConnection dbServer;
	private String ourIP;

	/**
	 * Public constructor: establishes a link to the underlying CouchDB server and
	 * creates all necessary tables.
	 * 
	 * @param dbServer  The CouchDB connection instance
	 * @param busName   Name of our message bus
	 */
	public CouchDbMessageBus( CouchDBConnection dbServer, String busName ) {
		this.busName  = busName;
		this.ourIP    = ourIP();
		this.dbServer = dbServer;

		try {
			if( ! this.dbServer.dbExists( busName )) {
				this.dbServer.dbCreate( busName );
			}
		} 
		catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Public constructor: establishes a link to the underlying CouchDB server and
	 * creates all necessary tables. If the server is secured, this constructor
	 * requires a configuration including username and password of an admin user (or
	 * a user with enough privileges to create tables).
	 * 
	 * @param config  The CouchDB server configuration
	 * @param busName Name of our message bus
	 */
	public CouchDbMessageBus( CouchDbConfig config, String busName ) {
		this( new CouchDBConnection( config ), busName );
	}

	/**
	 * Public constructor: establishes a link to the underlying CouchDB server and
	 * creates all necessary tables. If the server is secured, this constructor
	 * requires username and password of an admin user (or a user with enough
	 * privileges to create tables).
	 * 
	 * @param dbURL     URL of the CouchDB server
	 * @param username  Username of admin account on the CouchDB server
	 * @param password  Password of admin account on the CouchDB server
	 * @param busName   Name of our message bus
	 */
	public CouchDbMessageBus( String dbURL, String username, String password, String busName ) {
		this( new CouchDbConfig( dbURL, username, password ), busName );
	}


	@Override
	public Envelope[] find( String query ) throws IOException {

		Envelope[] records = 
				(Envelope[]) dbServer.find( busName, Envelope[].class, query );		
		return records;
	}
	
	@Override
	public Envelope findNext(String queueName) throws IOException {
		return findNext( queueName, 0 );
	}
	
	@Override
	public Envelope findNext(String queueName, int timeout) throws IOException, TimeoutException {
		
		Date callTime = new Date();	// TODO -- add progressive waiting time
		
		String query = "{ 'selector':  { '$and': [ { 'queueName': { '$regex' : '" 
				+ queueName
				+ "' }}, { 'consumed': false } ] }}";
		query = query.replace( '\'', '\"' );
		while( true ) {
			// Did we time out?
			Date now = new Date();
			if( timeout > 0 && (now.getTime() - callTime.getTime()) > timeout ) {
				throw new TimeoutException( "After " + timeout + "msec" );
			}
			
			Envelope[] messages = dbServer.find( busName, Envelope[].class, query );
			if( messages.length > 0 ) {
				Arrays.sort( messages );
				Envelope ret = messages[0];
				ret.setConsumed( true );
				dbServer.save( busName, ret );
				return ret;
			}
			sleep( 1000 );
		}
	}
	
	/** @return Our internal {@link CouchDBConnection} instance */
	public CouchDBConnection getDbServer() {
		return dbServer;
	}

	@Override
	public List<String> groupMembers( String groupName ) throws IOException {
		ReceiverGroup g = dbServer.findOne( busName, ReceiverGroup.class, groupName );
		return g.getMembers();
	}
	
	@Override
	public void joinGroup( String queueName, String groupName ) {
		
		if( groupName == null || (!groupName.endsWith( ".*" ))) {
			throw new IllegalArgumentException( "Invalid group name: " + groupName );
		}
		if( queueName == null ) {
			throw new IllegalArgumentException( "Null queueName" );
		}
		
		// First let's see if that group exists, otherwise we'll create it
		try {
			ReceiverGroup group = dbServer.findOne( busName, ReceiverGroup.class, groupName );
			if( group == null ) {
				// No group yet, let's create it first
				group = new ReceiverGroup( groupName );
			}

			// Now we add ourselves to the group  
			group.add( queueName );
			dbServer.save( busName, group );
		} 
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public MessageQueue messageQueue(String queueName) {
		MessageQueue ret = new MessageQueue( queueName, this );
		return ret;
	}

	@Override
	public Envelope send( String queueName, Message message ) {

		if( queueName == null || message == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		// Are we sending to a group?
		if( ! queueName.endsWith( ".*" )) {
			Envelope ret = sendOne( queueName, message );		// No, just send this message
			return ret;
		}
		
		// We are sending to a group: loop over all recipients
		try {
			ReceiverGroup recGroup = this.dbServer.findOne( queueName, ReceiverGroup.class, queueName );
			if( recGroup == null ) {
				throw new RuntimeException("Receiver group '" + queueName + "' not found");
			}
			Envelope ret = null;
			for( String member: recGroup.getMembers() ) {
				ret = sendOne( member, message );
			}
			return ret;
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and sends it.
	 */
	private Envelope sendOne( String queueName, Message message ) {
		Envelope envelope = new Envelope( message, nowISO(), ourIP, queueName );
		try {
			dbServer.save( busName, envelope );
			return envelope;
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public void listen( String queueName, 
						MessageConsumer consumer, 
						int timeout, 
						boolean metadata, 
						boolean justOne ) throws IOException {
		
		if( queueName == null || consumer == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		while( true ) {
			Envelope msg = findNext( queueName, timeout );
			consumer.consume( metadata ? msg : msg.getMessage() );
			if( justOne ) {
				break;
			}
		}
	}


	@Override
	public Thread listenInThread( String queueName, MessageConsumer consumer, int timeout, boolean metadata,
			boolean justOne) {
		
		Runnable receiver = () -> {	
			try {
				this.listen( queueName, consumer, timeout, metadata, justOne );
			}
			catch ( TimeoutException e ) {
				// ignore
			}
			catch ( Exception e ) {
				throw new RuntimeException( e );
			}
		};
		Thread t = new Thread( receiver );
		t.start();
		return t;
	}
}
