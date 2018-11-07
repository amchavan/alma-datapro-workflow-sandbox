package alma.obops.draws.messages;

import java.io.IOException;

import alma.obops.draws.messages.Envelope.State;

/**
 * Allows messages to be sent and subscribed to. Depends on a {@link MessageBroker}
 * providing the transport mechanism.
 * 
 * @author mchavan, 18-Sep-2018
 */
public class MessageQueue {
	
	private String queueName;
	private MessageBroker messageBroker;
	
	/**
	 * @param name
	 *            If it ends with <code>.*</code> it is interpreted as a group ID;
	 *            messages set to the queue will be sent to all group members
	 */
	public MessageQueue( String queueName, MessageBroker messageBus ) {
		if( queueName == null || messageBus == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		this.queueName = queueName;
		this.messageBroker = messageBus;
	}
	
	public MessageBroker getMessageBroker() {
		return messageBroker;
	}
	
	public String getName() {
		return queueName;
	}

	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and sends to this queue. <br>
	 * The {@link Envelope} and {@link Message} instances reference each other.<br>
	 * The {@link Message} instance is set to {@link State#Sent}.
	 */
	public Envelope send( Message message ) {
		return this.send( message, 0 );
	}
	
	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and sends to this queue. <br>
	 * The {@link Envelope} and {@link Message} instances reference each other.<br>
	 * The {@link Message} instance is set to {@link State#Sent}.
	 * 
	 * @param timeToLive
	 *            The time before this instance expires, in msec; if
	 *            <code>null</code>, this instance never expires
	 */
	public Envelope send( Message message, long timeToLive ) {
		return messageBroker.send( this, message, timeToLive );
	}
	
	/**
	 * Delete this queue and all its messages
	 * 
	 * @throws IOException 
	 */
	public void delete() throws IOException {
		messageBroker.deleteQueue( this );
	}
	
	/**
	 * Search a queue for for new messages, return the oldest we can find
	 * 
	 * @return An {@link Envelope} wrapping a user {@link Message}.
	 * 
	 * @throws IOException 
	 */
	public Envelope receive() throws IOException {
		return messageBroker.receive( this );
	}
	
	/**
	 * Search a queue for for new messages, return the oldest we can find
	 * 
	 * @param timeout
	 *            If timeout > 0 it represents the number of msec to wait for a
	 *            message to arrive before timing out: upon timeout a
	 *            {@link TimeLimitExceededException} is thrown.
	 * 
	 * @return An {@link Envelope} wrapping a user {@link Message}.
	 * 
	 * @throws TimeLimitExceededException If waiting time exceeded the given timeout value
	 * @throws IOException
	 */
	public Envelope receive( int timeout ) throws IOException, TimeLimitExceededException {
		return messageBroker.receive( this, timeout );
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
		messageBroker.joinGroup( queueName, groupName );
	}

	/**
	 * Listen for messages and process them as they come in.<br>
	 * This method times out.
	 * 
	 * @param consumer
	 *            Callback function to process the message with
	 *            
	 * @param timeout
	 *            If timeout is not-<code>null</code> and positive it represents the
	 *            number of msec to wait for a message to arrive before timing out;
	 *            upon timeout a
	 *            {@link TimeLimitExceededException} is thrown
	 * @param justOne
	 *            If <code>true</code>, return after the first message
	 *            
	 * @throws IOException
	 * @throws TimeLimitExceededException If waiting time exceeded the given timeout value
	 */
	public void listen( MessageConsumer consumer, int timeout ) throws IOException, TimeLimitExceededException {
		this.messageBroker.listen( this, consumer, timeout );
	} 	
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + queueName + "]";
	}

	/**
	 * Start a background thread listening for messages matching the
	 * queue name and processing them as they come in.<br>
	 * This method times out.<br>
	 * This method is a wrapper around {@link #listen()}.
	 */
	public Thread listenInThread( MessageConsumer consumer, int timeout ) {
		return messageBroker.listenInThread( this, consumer, timeout );
	}
}
