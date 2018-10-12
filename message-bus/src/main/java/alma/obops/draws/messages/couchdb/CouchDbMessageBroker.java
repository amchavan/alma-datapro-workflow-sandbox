package alma.obops.draws.messages.couchdb;

import static alma.obops.draws.messages.MessageBroker.now;
import static alma.obops.draws.messages.MessageBroker.nowISO;
import static alma.obops.draws.messages.MessageBroker.ourIP;
import static alma.obops.draws.messages.MessageBroker.sleep;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import alma.obops.draws.messages.AbstractMessageBroker;
import alma.obops.draws.messages.DbConnection;
import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.Envelope.State;
import alma.obops.draws.messages.Message;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.Record;
import alma.obops.draws.messages.RequestMessage;
import alma.obops.draws.messages.SimpleEnvelope;
import alma.obops.draws.messages.TimeLimitExceededException;

public class CouchDbMessageBroker extends AbstractMessageBroker implements MessageBroker {
	    
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
	public CouchDbMessageBroker( DbConnection dbConn, String busName ) {
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
	public CouchDbMessageBroker( CouchDbConfig config, String busName ) {
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
	public CouchDbMessageBroker( String dbURL, String username, String password, String busName ) {
		this( new CouchDbConfig( dbURL, username, password ), busName );
	}


	/**
	 * Search for messages; selector includes the query parameters. Returned
	 * messages are not consumed.
	 * 
	 * @param query
	 *            A JSON selector like
	 *            <pre>{ "selector": { "message": { "$exists": true }}}</pre>
	 *            It's important that the selector restricts the result set to
	 *            include only messages, as in this example.
	 * 
	 *            See also http://docs.couchdb.org/en/2.1.1/api/database/find.html
	 * 
	 * @return A possibly empty array of documents
	 */
	public SimpleEnvelope[] find( String query ) throws IOException {

		SimpleEnvelope[] envelopes = 
				(SimpleEnvelope[]) dbConn.find( busName, SimpleEnvelope[].class, query );

		return envelopes;
	}
	
	@Override
	// TODO -- add progressive waiting time
	public SimpleEnvelope receive( MessageQueue queue, long timeLimit ) throws IOException, TimeLimitExceededException {
				
		Date callTime = now();
		String queryFmt = "{'selector': {'$and': [{'queueName':'%s'}, {'state':'Sent'}]}}";		
		String query = String.format(queryFmt, queue.getName() ).replace( '\'', '\"' );
		
		while( true ) {
			
			// Did we time out?
			Date now = now();
			if( timeLimit > 0 && (now.getTime() - callTime.getTime()) > timeLimit ) {
				// YES, throw an exception and exit
				throw new TimeLimitExceededException( "After " + timeLimit + "msec" );
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
					dbConn.save( busName, new CouchDbEnvelope( ret ));
					return ret;
				}
			}
			sleep( 1000 );	// TODO: make this dynamic
		}
	}
	
	/** @return Our internal {@link DbConnection} instance */
	public DbConnection getDbConnection() {
		return dbConn;
	}

	@Override
	public List<String> groupMembers( String groupName ) throws IOException {
		if( groupName == null || (!groupName.endsWith( ".*" ))) {
			throw new IllegalArgumentException( "Invalid group name: " + groupName );
		}

		ReceiverGroup g = dbConn.findOne( busName, ReceiverGroup.class, groupName );
		if( g != null ) {
			return g.getMembers();
		}
		
		return null;
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
	public Envelope sendOne( MessageQueue queue, Message message, long timeToLive ) {
		CouchDbEnvelope envelope = new CouchDbEnvelope( message, ourIP, queue.getName(), timeToLive );
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
	// TODO: pull this up to AbstractMessageBroker, need to harmonize
	//       with RabbitMqMessageBroker -- only difference is how 
	//		 envelope state changes are persisted
	public void listen( MessageQueue queue, 
						MessageConsumer consumer, 
						int timeout, 
						boolean justOne ) throws IOException {
		
		if( queue == null || consumer == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		while( true ) {
			
			// Receive the next message, consume it, and mark it as such
			SimpleEnvelope envelope = receive( queue, timeout );
			consumer.consume( envelope.getMessage() );
			
			envelope.setState( State.Consumed );
			envelope.setConsumedTimestamp( nowISO() );
			dbConn.save( busName, new CouchDbEnvelope( envelope ));
			
			if( justOne ) {
				break;
			}
		}
	}

	/**
	 * Mark as {@link State#Expired} all {@link Envelope} instances in the given
	 * queue for which {@link Envelope#getTimeToLive()} returns 0.<br>
	 * 
	 * @return The number of expired messages
	 * @throws IOException 
	 */
	public int purgeExpiredMessages( String queueName ) throws IOException {
		
		String queryFmt = "{'selector': {'$and': [{'queueName':'%s'}, {'state':'Sent'}]}}";		
		String query = String.format(queryFmt, queueName ).replace( '\'', '\"' );
		
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

	private void purgeExpiredMessage( SimpleEnvelope envelope ) throws IOException {
		envelope .setState( State.Expired );
		envelope .setExpiredTimestamp( nowISO() );
		dbConn.save( busName, (Record) envelope  );
		System.out.println( ">>> Expired: " + envelope  );
	}
}
