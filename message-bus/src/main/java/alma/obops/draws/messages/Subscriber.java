package alma.obops.draws.messages;

import java.io.IOException;

/**
 * Subscribes to messages sent to a given address
 * @author mchavan, 07-Jan-2019
 */
public class Subscriber {

	private MessageBroker messageBroker;
	private MessageQueue queue;

	
	public MessageQueue getQueue() {
		return queue;
	}

	/**
	 * @param messageBroker
	 * @param queueName     Address of the messages we subscribe to
	 * @param serviceName   Identifies the service that's subscribing, as multiple
	 *                      services could subscribe to the same messages
	 */
	public Subscriber( MessageBroker messageBroker, String queueName, String serviceName ) {
		this.messageBroker = messageBroker;
		this.messageBroker.setServiceName( serviceName );
		this.queue = messageBroker.messageQueue( queueName );
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
		this.messageBroker.listen( this.queue, consumer, timeout );
	}
	
	/**
	 * Start a background thread listening for messages matching the
	 * queue name and processing them as they come in.<br>
	 * This method times out.<br>
	 * This method is a wrapper around {@link #listen()}.
	 */
	public Thread listenInThread( MessageConsumer consumer, int timeout ) {
		return messageBroker.listenInThread( this.queue, consumer, timeout );
	}
	
	/**
	 * Search a queue for for new messages, return the oldest we can find
	 * 
	 * @return An {@link Envelope} wrapping a user {@link Message}.
	 * 
	 * @throws IOException 
	 */
	public Envelope receive() throws IOException {
		return messageBroker.receive( queue );
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
		return messageBroker.receive( queue, timeout );
	}
}
