package alma.icd.adapt.messagebus;

import java.io.IOException;
import java.net.Inet4Address;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import alma.icd.adapt.messagebus.Envelope.State;
import alma.icd.adapt.messagebus.security.TokenFactory;

/**
 * Describes the functions implementing the message passing machinery.
 * @author mchavan
 */
public interface MessageBroker {

	public static final String DEFAULT_MESSAGE_BROKER_NAME = "alma";
	
	/** All timestamps are in Universal Time (UT) */
    public static final TimeZone UT = TimeZone.getTimeZone( "Etc/GMT" );

    /**
     * Shorter ISO format for date/time strings: no time zone.
     */
    public static final String ISOTIMEDATESTRING = 
        "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * @return The current date/time in UT
     */
    public static Date now() {
        Calendar calendar = Calendar.getInstance( UT );
        return calendar.getTime();
    }
    
	/**
	 * @return The current date/time in ISO short format, for instance <code>2018-09-18T13:48:31</code>
	 */
	public static String nowISO() {
        SimpleDateFormat dateFormat = new SimpleDateFormat( ISOTIMEDATESTRING );
        dateFormat.setTimeZone( UT );
        String ret = dateFormat.format( now() );
        return ret;
    }
	
	/**
	 * @return Our IP address if it can be computed, <code>0.0.0.0</code> otherwise
	 */
	public static String ourIP() {
		String ret = null;
		try {
			ret = Inet4Address.getLocalHost().getHostAddress();
		} 
		catch( Exception e ) {
			e.printStackTrace();
			ret = "0.0.0.0";
		}
        return ret;
    }
	
	/**
	 * @param isoDatetime A datatime string in ISO short format, for instance <code>2018-09-18T13:48:31</code>
	 * @return The corresponding Date
	 * @throws ParseException 
	 */
	public static Date parseIsoDatetime( String isoDatetime ) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat( ISOTIMEDATESTRING );
        dateFormat.setTimeZone( UT );
		Date ret = dateFormat.parse( isoDatetime ); 
		return ret;
	}

	/**
	 * A simple wrapper around {@link Thread#sleep(long)} that doesn't throw checked
	 * exceptions
	 */
	public static void sleep( long msec ) {
		try {
			Thread.sleep( msec );
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}
	
	/**
	 * Close the connection to the broker — may be necessary to correctly terminate
	 * a client
	 */
	public void closeConnection();

	/**
	 * Delete a queue and all its messages. Use with care.
	 */
	public void deleteQueue( MessageQueue queue );
	
	/**
	 * @return The broker's token factory: message passing is only secured if a
	 *         token factory is associated with the broker. If <code>null</code>,
	 *         message security is turned off; that's the default condition.
	 */
	public TokenFactory getTokenFactory();

	/**
	 * Look for group members.
	 * 
	 * @param groupName
	 *            Name of the group to interrogate, should be non-<code>null</code>
	 *            and end with '<code>.*</code>', e.g. <code>state.changes.*</code>
	 * 
	 * @return A possibly empty list of members (queue names) of that group, or
	 *         <code>null</code> if that group does not exist.
	 * @throws IOException 
	 */
	public List<String> groupMembers( String groupName  ) throws IOException;

	/**
	 * Listen for messages matching the queue name and process them as
	 * they come in.<br>
	 * This method never returns.
	 * 
	 * @param consumer
	 *            Callback function to process the message with
	 * @param queueName
	 *            Defines what messages to listen to.
//	 * @param condition
//	 *            Boolean function to be invoked before starting to listen: if not
//	 *            <code>null</code>will cause the thread to sleep if the condition
//	 *            is false
	 */
//	public void listen( String queueName, MessageConsumer consumer ) throws IOException;

	/**
	 * Add a queue to a group: messages sent to the group will be passed on to
	 * that queue as well.
	 * 
	 * @param groupName
	 *            Name of the group to join, should be non-<code>null</code> and end
	 *            with '<code>.*</code>', e.g. <code>state.changes.*</code>
	 */
	public void joinGroup( String queueName, String groupName  );

	/**
	 * Listen for messages matching the queue name and process them as
	 * they come in.<br>
	 * This method times out.
	 * 
	 * @param consumer
	 *            Callback function to process the message with
	 * @param queue
	 *            Defines what messages to listen to. If <code>null</code>, defaults
	 *            to the value of this class' {@link #listenTo} field
// Parameter condition is not yet implemented
//	 * @param condition
//	 *            Boolean function to be invoked before starting to listen: if not
//	 *            <code>null</code>will cause the thread to sleep if the condition
//	 *            is false
	 * @param timeout
	 *            If timeout > 0 it represents the number of msec to wait for a
	 *            message to arrive before timing out: upon timeout a
	 *            {@link TimeLimitExceededException} is thrown
	 *            
	 * @throws TimeLimitExceededException
	 *             If waiting time exceeded the given limit
	 * @throws IOException
	 */
	public void listen( MessageQueue queue, 
						MessageConsumer consumer, 
						int timeout ) throws IOException, TimeLimitExceededException;
		
	/**
	 * Start a background thread listening for messages matching the
	 * queue name and processing them as they come in.<br>
	 * This method times out.<br>
	 * This method is a wrapper around {@link #listen()}.
	 */
	public Thread listenInThread( MessageQueue queue, 
								  MessageConsumer consumer, 
								  int timeout );	

	/**
	 * @return A {@link MessageQueue} with the given name and service name
	 * 
	 * @param serviceName Identifies the service (application) that's subscribing,
	 *                    as multiple services could subscribe to the same messages.
	 *                    <br>
	 *                    Must be a valid C/Python/Java variable name. <br>
	 *                    Must be unique system-wide.
	 * 
	 */
	public MessageQueue messageQueue( String queueName, String serviceName );
	
	/**
	 * Find the next message of this queue: that is, the oldest
	 * non-{@link State#Received} message. That message will be set to
	 * {@link State#Received}, unless its time-to-live is down to zero, in
	 * which case it's set to {@linkplain State#Expired}.<br>
	 * This method will block indefinitely until a message is received.
	 * 
	 * @return An {@link Envelope} wrapping a user {@link Message}.
	 * 
	 * @throws IOException
	 */
	public Envelope receive( MessageQueue queue ) throws IOException;

	/**
	 * Find the next message of the queue, that is, the oldest
	 * non-{@link State#Received} message. That message will be set to
	 * {@link State#Received}, unless its time-to-live is down to zero, in which
	 * case it's set to {@linkplain State#Expired}.<br>
	 * This method will block until a message is received or the time limit is
	 * exceeded.
	 * 
	 * @param timeLimit
	 *            If greater than 0 it represents the number of msec to wait for a
	 *            message to arrive before timing out: upon timeout a
	 *            {@link TimeLimitExceededException} is thrown.
	 * 
	 * @return An {@link Envelope} wrapping a user {@link Message}.
	 * 
	 * @throws TimeLimitExceededException
	 *             If waiting time exceeded the given limit
	 * @throws IOException
	 */
	public Envelope receive( MessageQueue queue, long timeLimit ) throws IOException, TimeLimitExceededException;
	
	/**
	 * Give the broker a token factory: message passing is only secured if a token
	 * factory is associated with the broker.
	 * 
	 * @param factory A token factory: if <code>null</code>, message security is
	 *                turned off; that's the default condition.
	 */
	public void setTokenFactory( TokenFactory factory );

	public default Envelope send(String queueName, Message message) {
		return send( queueName, message, 0 );
	}
	
	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and sends it to a queue.<br>
	 * The {@link Envelope} and {@link Message} instances reference each other.<br>
	 * The {@link Message} instance is set to {@link State#Sent}.
	 * 
	 * @param queueName
	 *            Name of the queue.<br>
	 *            <b>BROKEN</b> If the queue name ends with <code>.*</code>
	 *            it is interpreted as a group ID and the message is sent to all
	 *            group members
	 * 
	 * @param expireTime
	 *            Time interval before this instance expires, in msec; if
	 *            expireTime=0 this instance never expires
	 */
	public Envelope send( String queueName, Message message, long expireTime );

}
