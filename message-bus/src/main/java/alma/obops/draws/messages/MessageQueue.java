package alma.obops.draws.messages;

import java.io.IOException;

/**
 * Allows messages to be sent and subscribed to. Depends on a {@link MessageBus}
 * providing the transport mechanism.
 * 
 * @author mchavan, 18-Sep-2018
 */
public class MessageQueue {
	
	private String queueName;
	private MessageBus messageBus;
	
	/**
	 * @param name
	 *            If it ends with <code>.*</code> it is interpreted as a group ID;
	 *            messages set to the queue will be sent to all group members
	 */
	public MessageQueue( String queueName, MessageBus messageBus ) {
		if( queueName == null || messageBus == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		this.queueName = queueName;
		this.messageBus = messageBus;
	}
	
	public MessageBus getMessageBus() {
		return messageBus;
	}
	
	public String getName() {
		return queueName;
	}

	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and sends it.
	 */
	public Envelope send( Message message ) {
		return messageBus.send( queueName, message);
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
	public Envelope[] find( String query ) throws IOException {
		return messageBus.find( query );
	}
	
	/**
	 * Search a queue for for new messages, return the oldest we can find
	 * 
	 * @return An {@link Envelope} wrapping a user {@link Message}.
	 * 
	 * @throws IOException 
	 */
	public Envelope findNext() throws IOException {
		return messageBus.findNext( queueName );
	}
	
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
	public Envelope findNext( int timeout ) throws IOException, TimeoutException {
		return messageBus.findNext( queueName, timeout );
	}

	
	/**
	 * Add this queue to a group: messages sent to the group will be passed on to
	 * this queue as well.
	 * 
	 * @param groupName
	 *            Name of the group to join, should be non-<code>null</code> and end
	 *            with '<code>.*</code>', e.g. <code>state.changes.*</code>
	 */
	public void joinGroup( String groupName ) {
		messageBus.joinGroup( queueName, groupName );
	}

	/**
	 * Listen for messages and process them as they come in.<br>
	 * This method times out.
	 * 
	 * @param consumer
	 *            Callback function to process the message with
//	 * @param condition
//	 *            Boolean function to be invoked before starting to listen: if not
//	 *            <code>null</code>will cause the thread to sleep if the condition
//	 *            is false
	 * @param timeout
	 *            If timeout > 0 it represents the number of msec to wait for a
	 *            message to arrive before timing out -- upon timeout a
	 *            RuntimeException is thrown
	 * @param metadata
	 *            If <code>true</code>, pass the consumer an {@link Envelope}
	 *            including all meta-data; otherwise a plain {@link Message}
	 * @param justOne
	 *            If <code>true</code>, return after the first message
	 * @throws IOException 
	 */
	public void listen( MessageConsumer consumer, int timeout, boolean metadata, boolean justOne ) throws IOException {
		messageBus.listen( queueName, consumer, timeout, metadata, justOne );
	} 	
	
	/**
	 * Start a background thread listening for messages matching the
	 * queue name and processing them as they come in.<br>
	 * This method times out.<br>
	 * This method is a wrapper around {@link #listen()}.
	 */
	public Thread listenInThread( MessageConsumer consumer, 
								  int timeout, 
								  boolean metadata, 
								  boolean justOne ) {
		
		return messageBus.listenInThread( queueName, 
										  consumer, 
										  timeout, 
										  metadata, 
										  justOne );
	}
}
