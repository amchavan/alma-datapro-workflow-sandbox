package alma.obops.draws.messages.couchdb;

import static alma.obops.draws.messages.MessageBus.now;
import static alma.obops.draws.messages.MessageBus.nowISO;
import static alma.obops.draws.messages.MessageBus.ourIP;
import static alma.obops.draws.messages.MessageBus.sleep;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import alma.obops.draws.messages.DbConnection;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.MessageBus;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.TimeoutException;

public class CouchDbMessageBus implements MessageBus {
	    
	private String busName;
	private DbConnection dbConn;
	private String ourIP;

	/**
	 * Public constructor: establishes a link to the underlying CouchDB server and
	 * creates all necessary tables.
	 * 
	 * @param dbConn   The CouchDB connection instance
	 * @param busName  Name of our message bus
	 */
	public CouchDbMessageBus( DbConnection dbConn, String busName ) {
		this.busName = busName;
		this.ourIP   = ourIP();
		this.dbConn  = dbConn;

		try {
			if( ! this.dbConn.dbExists( busName )) {
				this.dbConn.dbCreate( busName );
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
		this( new CouchDbConnection( config ), busName );
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
	public CouchDbEnvelope[] find( String query ) throws IOException {

		CouchDbEnvelope[] envelopes = 
				(CouchDbEnvelope[]) dbConn.find( busName, CouchDbEnvelope[].class, query );

//		// Message classes are not serialized so we need to restore them here
//		for( CouchDbEnvelope envelope : envelopes) {
//			Message message = envelope.getMessage();
//			if( message != null ) {
//				envelope.setMessageClass( message.getClass().getName() );
//			}
//		}
		return envelopes;
	}
	
	@Override
	public CouchDbEnvelope receive(String queueName) throws IOException {
		return receive( queueName, 0 );
	}
	
	@Override
	// TODO -- add progressive waiting time
	public CouchDbEnvelope receive( String queueName, int timeout ) throws IOException, TimeoutException {
				
		Date callTime = now();	
		String query = "{ 'selector':  { '$and': [ { 'queueName': { '$regex' : '" 
				+ queueName
				+ "' }}, { 'state': 'Sent' } ] }}";
		query = query.replace( '\'', '\"' );
		
		while( true ) {
			
			// Did we time out?
			Date now = now();
			if( timeout > 0 && (now.getTime() - callTime.getTime()) > timeout ) {
				// YES, throw an exception and exit
				throw new TimeoutException( "After " + timeout + "msec" );
			}
			
			// No timeout, let's try again
			CouchDbEnvelope[] envelopes = dbConn.find( busName, CouchDbEnvelope[].class, query );
			if( envelopes.length > 0 ) {
				
				Arrays.sort( envelopes );
				CouchDbEnvelope ret = null;
				
				// Purge all expired messages
				for( CouchDbEnvelope envelope : envelopes ) {
					if( envelope.getTimeToLive() != 0 ) {	// is it expired?
						ret = envelope;						// NO, return that
						break;
					}
					purgeExpiredMessage( envelope );		// YES, purge it
				}

				// Do we have a non-expired message?
				if( ret != null ) {
					// YES, return that as "Received"
					ret.setState( State.Received );
					ret.setReceivedTimestamp( nowISO() );
					dbConn.save( busName, ret );
					return ret;
				}
			}
			sleep( 1000 );
		}
	}
	
	/** @return Our internal {@link DbConnection} instance */
	public DbConnection getDbConnection() {
		return dbConn;
	}

	@Override
	public List<String> groupMembers( String groupName ) throws IOException {
		ReceiverGroup g = dbConn.findOne( busName, ReceiverGroup.class, groupName );
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
			ReceiverGroup group = dbConn.findOne( busName, ReceiverGroup.class, groupName );
			if( group == null ) {
				// No group yet, let's create it first
				group = new ReceiverGroup( groupName );
			}

			// Now we add ourselves to the group  
			group.add( queueName );
			dbConn.save( busName, group );
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
	public CouchDbEnvelope send( String queueName, Message message ) {
		return send( queueName, message, null );
	}

	@Override
	public CouchDbEnvelope send( String queueName, Message message, Long timeToLive ) {

		if( queueName == null || message == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		// Are we sending to a group?
		if( ! queueName.endsWith( ".*" )) {
			CouchDbEnvelope ret = sendOne( queueName, message, timeToLive );		// No, just send this message
			return ret;
		}
		
		// We are sending to a group: loop over all recipients
		try {
			ReceiverGroup recGroup = this.dbConn.findOne( queueName, ReceiverGroup.class, queueName );
			if( recGroup == null ) {
				throw new RuntimeException("Receiver group '" + queueName + "' not found");
			}
			CouchDbEnvelope ret = null;
			for( String member: recGroup.getMembers() ) {
				ret = sendOne( member, message, timeToLive );
			}
			return ret;
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Creates an {@link CouchDbEnvelope} (including meta-data) from the given
	 * {@link Message} and sends it.
	 */
	private CouchDbEnvelope sendOne( String queueName, Message message, Long timeToLive ) {
		CouchDbEnvelope envelope = new CouchDbEnvelope( message, ourIP, queueName, timeToLive );
		envelope.setSentTimestamp( nowISO() );
		envelope.setState( State.Sent );
		envelope.setMessageClass( message.getClass().getName() );
		message.setEnvelope( envelope );
		try {
			dbConn.save( busName, envelope );
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
						boolean justOne ) throws IOException {
		
		if( queueName == null || consumer == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		while( true ) {
			
			// Receive the next message, consume it, and mark it as such
			CouchDbEnvelope envelope = receive( queueName, timeout );
			consumer.consume( envelope.getMessage() );
			
			envelope.setState( State.Consumed );
			envelope.setConsumedTimestamp( nowISO() );
			dbConn.save( busName, envelope );
			
			if( justOne ) {
				break;
			}
		}
	}


	@Override
	public Thread listenInThread( String queueName, MessageConsumer consumer, int timeout,
			boolean justOne) {
		
		Runnable receiver = () -> {	
			try {
				this.listen( queueName, consumer, timeout, justOne );
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

	@Override
	public int purgeExpiredMessages( String queueName ) throws IOException {
		
		String query = "{ 'selector':  { '$and': [ { 'queueName': { '$regex' : '" 
				+ queueName
				+ "' }}, { 'state': 'Sent' } ] }}";
		query = query.replace( '\'', '\"' );
		
		CouchDbEnvelope[] envelopes = dbConn.find( busName, CouchDbEnvelope[].class, query );
		int ret = 0;
		for( CouchDbEnvelope envelope: envelopes ) {
			if( envelope.getTimeToLive() == 0 ) {
				purgeExpiredMessage( envelope );
				ret++;
			}
		}
		
		return ret;
	}

	private void purgeExpiredMessage(CouchDbEnvelope envelope) throws IOException {
		envelope.setState( State.Expired );
		envelope.setExpiredTimestamp( nowISO() );
		dbConn.save( busName, envelope );
		System.out.println( ">>> Expired: " + envelope );
	}
}
