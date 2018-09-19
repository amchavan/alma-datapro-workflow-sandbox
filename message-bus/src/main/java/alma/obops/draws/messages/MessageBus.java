package alma.obops.draws.messages;

import java.io.IOException;
import java.net.Inet4Address;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

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
	 * @return the current datetime in ISO short format, for instance <code>2018-09-18T13:48:31</code>
	 */
	public static String nowISO() {
        SimpleDateFormat dateFormat = new SimpleDateFormat( ISOTIMEDATESTRING_SHORT );
        dateFormat.setTimeZone( UT );
        Calendar calendar = Calendar.getInstance( UT );
        String ret = dateFormat.format( calendar.getTime() );
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
	 * Search a queue for for new messages, return the oldest we can find
	 * 
	 * @return An {@link Envelope} wrapping a user {@link Message}.
	 * 
	 * @throws IOException 
	 */
	public Envelope findNext( String queueName ) throws IOException;

	/**
	 * Search a queue for for new messages, return the oldest we can find
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
	public Envelope findNext( String queueName, int timeout ) throws IOException, TimeoutException;

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
	 * @param metadata
	 *            If <code>true</code>, pass the consumer an {@link Envelope}
	 *            including all meta-data; otherwise a plain {@link Message}
	 * @param justOne
	 *            If <code>true</code>, return after the first message
	 */
	public void listen( String queueName, 
						MessageConsumer consumer, 
						int timeout, 
						boolean metadata, 
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
								  boolean metadata, 
								  boolean justOne );	
	
    /**
	 * @return A {@link MessageQueue} with the given name
	 */
	public MessageQueue messageQueue( String queueName );

	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and sends it to a queue.
	 * 
	 * @param queueName
	 *            Name of the queue to send it to. If it ends with <code>.*</code>
	 *            it is interpreted as a group ID and the message is sent to all
	 *            group members
	 */
	public Envelope send( String queueName, Message message );

}
