package alma.obops.draws.messages;

import java.io.IOException;
import java.net.Inet4Address;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import alma.obops.draws.messages.Envelope.State;

/**
 * Describes the functions implementing the message passing machinery.
 * @author mchavan
 */
public interface MessageBus {

	/** All timestamps are in Universal Time (UT) */
    public static final TimeZone UT = TimeZone.getTimeZone( "Etc/GMT" );

    /**
     * Shorter ISO format for date/time strings: no milliseconds nor time zone.
     */
    public static final String ISOTIMEDATESTRING_SHORT = 
        "yyyy-MM-dd'T'HH:mm:ss";

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
        SimpleDateFormat dateFormat = new SimpleDateFormat( ISOTIMEDATESTRING_SHORT );
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
        SimpleDateFormat dateFormat = new SimpleDateFormat( ISOTIMEDATESTRING_SHORT );
        dateFormat.setTimeZone( UT );
		Date ret = dateFormat.parse( isoDatetime ); 
		return ret;
	}

	/**
	 * A simple wrapper around {@link Thread#sleep(long)}, swallows the dreaded
	 * {@link InterruptedException}
	 */
	public static void sleep(int msec) {
		try {
			Thread.sleep( msec );
		}
		catch (InterruptedException e) {
			// ignore
		}
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
	public Envelope[] find( String query ) throws IOException;

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
	 * @param selector
	 *            Defines what messages to listen to. If <code>null</code>, defaults
	 *            to the value of this class' {@link #listenTo} field
	 * @param condition
	 *            Boolean function to be invoked before starting to listen: if not
	 *            <code>null</code>will cause the thread to sleep if the condition
	 *            is false
	 * @param timeout
	 *            If timeout > 0 it represents the number of msec to wait for a
	 *            message to arrive before timing out -- upon timeout a
	 *            RuntimeException is thrown
	 * @param justOne
	 *            If <code>true</code>, return after the first message
	 */
	public void listen( String queueName, 
						MessageConsumer consumer, 
						int timeout, 
						boolean justOne ) throws IOException;

	/**
	 * Start a background thread listening for messages matching the
	 * queue name and processing them as they come in.<br>
	 * This method times out.<br>
	 * This method is a wrapper around {@link #listen()}.
	 */
	public Thread listenInThread( String queueName, 
								  MessageConsumer consumer, 
								  int timeout, 
								  boolean justOne );

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
	 * @return A {@link MessageQueue} with the given name
	 */
	public MessageQueue messageQueue( String queueName );
	
	/**
	 * Mark as {@link State#Expired} all {@link Envelope} instances in the given
	 * queue for which {@link Envelope#getTimeToLive()} returns 0.<br>
	 * 
	 * @return The number of expired messages
	 * @throws IOException 
	 */
	public int purgeExpiredMessages( String queueName ) throws IOException;	
	
	/**
	 * Find the next message of this queue: that is, the oldest
	 * non-{@link State#Received} message. That message will be set to
	 * {@link State#Received}, unless its time-to-live is down to zero, in
	 * which case it's set to {@linkplain State#Expired}.
	 * 
	 * @return An {@link Envelope} wrapping a user {@link Message}.
	 * 
	 * @throws IOException
	 */
	public Envelope receive( String queueName ) throws IOException;	
	
    /**
	 * Find the next message of this queue, that is, the oldest
	 * non-{@link State#Received} message. That message will be set to
	 * {@link State#Received}, unless its time-to-live is down to zero, in
	 * which case it's set to {@linkplain State#Expired}.
	 * 
	 * @param timeout
	 *            If timeout > 0 it represents the number of msec to wait for a
	 *            message to arrive before timing out: upon timeout a
	 *            {@link TimeoutException} is thrown.
	 * 
	 * @return An {@link Envelope} wrapping a user {@link Message}.
	 * 
	 * @throws TimeoutException		If waiting time exceeded the given timeout value
	 * @throws IOException
	 */
	public Envelope receive( String queueName, int timeout ) throws IOException, TimeoutException;

	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and sends it to a queue.<br>
	 * The {@link Envelope} and {@link Message} instances reference each other.<br>
	 * The {@link Message} instance is set to {@link State#Sent}.
	 * 
	 * @param queueName
	 *            Name of the queue to send it to. If it ends with <code>.*</code>
	 *            it is interpreted as a group ID and the message is sent to all
	 *            group members
	 */
	public Envelope send( String queueName, Message message );

	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and sends it to a queue.<br>
	 * The {@link Envelope} and {@link Message} instances reference each other.<br>
	 * The {@link Message} instance is set to {@link State#Sent}.
	 * 
	 * @param queueName
	 *            Name of the queue to send it to. If it ends with <code>.*</code>
	 *            it is interpreted as a group ID and the message is sent to all
	 *            group members
	 * 
	 * @param timeToLive
	 *            The time before this instance expires, in msec; if
	 *            <code>null</code>, this instance never expires
	 */
	public Envelope send( String queueName, Message message, Long timeToLive );
}
