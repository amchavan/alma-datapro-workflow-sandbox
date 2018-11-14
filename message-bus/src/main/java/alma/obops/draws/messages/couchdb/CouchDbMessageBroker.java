package alma.obops.draws.messages.couchdb;

import static alma.obops.draws.messages.MessageBroker.now;
import static alma.obops.draws.messages.MessageBroker.nowISO;
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
import alma.obops.draws.messages.SimpleEnvelope;
import alma.obops.draws.messages.TimeLimitExceededException;

public class CouchDbMessageBroker extends AbstractMessageBroker implements MessageBroker {
	
	private String brokerName;
	private DbConnection dbConn;

	/**
	 * Public constructor: establishes a link to the underlying CouchDB server and
	 * creates all necessary tables. The broker's name will be {@value
	 * 
	 * @param dbConn   The CouchDB connection instance
	 * @param busName  Name of our message broker, will map to a CouchDB database name
	 */
	public CouchDbMessageBroker( DbConnection dbConn ) {
		this( dbConn, MessageBroker.DEFAULT_MESSAGE_BROKER_NAME );
	}

	/**
	 * Public constructor: establishes a link to the underlying CouchDB server and
	 * creates all necessary tables.
	 * 
	 * @param dbConn   The CouchDB connection instance
	 * @param busName  Name of our message broker, will map to a CouchDB database name
	 */
	public CouchDbMessageBroker( DbConnection dbConn, String brokerName ) {
		this.brokerName = brokerName;
		this.dbConn  = dbConn;

		try {
			if( ! this.dbConn.dbExists( brokerName )) {
				this.dbConn.dbCreate( brokerName );
			}
		} 
		catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}

//	/**
//	 * Public constructor: establishes a link to the underlying CouchDB server and
//	 * creates all necessary tables. If the server is secured, this constructor
//	 * requires username and password of an admin user (or a user with enough
//	 * privileges to create tables).
//	 * 
//	 * @param dbURL     URL of the CouchDB server
//	 * @param username  Username of admin account on the CouchDB server
//	 * @param password  Password of admin account on the CouchDB server
//	 * @param busName   Name of our message bus
//	 */
//	public CouchDbMessageBroker( String dbURL, String username, String password, String busName ) {
//		this( new CouchDbConfig( dbURL, username, password ), busName );
//	}


	/**
	 * Mark as {@link State#Expired} all {@link Envelope} instances in the given
	 * queue for which {@link Envelope#getTimeToLive()} returns 0.
	 * 
	 * @return The number of expired messages
	 * @throws IOException 
	 */
	public int expireMessages( String queueName ) throws IOException {
		
		String queryFmt = "{'selector': {'$and': [{'queueName':'%s'}, {'state':'Sent'}]}}";		
		String query = String.format(queryFmt, queueName ).replace( '\'', '\"' );
		
		CouchDbEnvelope[] envelopes = dbConn.find( brokerName, CouchDbEnvelope[].class, query );
		int ret = 0;
		for( CouchDbEnvelope envelope: envelopes ) {
			if( envelope.getTimeToLive() == 0 ) {
				this.setState( envelope, State.Expired );
				ret++;
			}
		}
		
		return ret;
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
				(SimpleEnvelope[]) dbConn.find( brokerName, SimpleEnvelope[].class, query );

		return envelopes;
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

		ReceiverGroup g = dbConn.findOne( brokerName, ReceiverGroup.class, groupName );
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
			ReceiverGroup group = dbConn.findOne( brokerName, ReceiverGroup.class, groupName );
			if( group == null ) {
				// No group yet, let's create it first
				group = new ReceiverGroup( groupName );
			}

			// Now we add ourselves to the group  
			group.add( queueName );
			dbConn.save( brokerName, group );
		} 
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public void listen( MessageQueue queue, 
						MessageConsumer consumer, 
						int timeout ) throws IOException {
		
		if( queue == null || consumer == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		while( true ) {
			
			// Receive the next message, consume it, and mark it as such
			SimpleEnvelope envelope = (SimpleEnvelope) receive( queue, timeout );
			consumer.consume( envelope.getMessage() );
			
			envelope.setState( State.Consumed );
			envelope.setConsumedTimestamp( nowISO() );
			dbConn.save( brokerName, new CouchDbEnvelope( envelope ));
		}
	}

	@Override
	protected SimpleEnvelope receiveOne( MessageQueue queue, long timeLimit ) throws TimeLimitExceededException {
				
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
			CouchDbEnvelope[] envelopes;
			try {
				envelopes = dbConn.find( brokerName, CouchDbEnvelope[].class, query );
			} 
			catch (IOException e) {
				throw new RuntimeException( e );
			}
			
			if( envelopes.length > 0 ) {	
				Arrays.sort( envelopes );
				CouchDbEnvelope ret = envelopes[0];
				return ret;
			}
			
			sleep( 1000L );		// TODO -- make this a progressive waiting time
		}
	}

	@Override
	protected SimpleEnvelope sendOne( MessageQueue queue, Message message, long timeToLive ) {
		SimpleEnvelope se = super.sendOne( queue, message, timeToLive );
		CouchDbEnvelope envelope = new CouchDbEnvelope( se );
		try {
			dbConn.save( brokerName, envelope );
			return envelope;
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Subclassed to support persisting the new state
	 */
	@Override
	protected String setState( Envelope envelope, State state ) throws IOException {
		String timestamp = super.setState(envelope, state);
		dbConn.save( brokerName, (Record) envelope  );
		return timestamp;
	}
}
