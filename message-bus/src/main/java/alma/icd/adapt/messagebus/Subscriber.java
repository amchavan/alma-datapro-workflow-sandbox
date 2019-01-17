package alma.icd.adapt.messagebus;

import java.io.IOException;
import java.util.List;

import alma.icd.adapt.messagebus.security.TokenFactory;

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
	 * @param queueName   Address of the messages we subscribe to
	 * 
	 * @param serviceName Identifies the service (application) that's subscribing,
	 *                    as multiple services could subscribe to the same messages.
	 *                    <br>
	 *                    Must be a valid C/Python/Java variable name. <br>
	 *                    Must be unique system-wide.
	 */
	public Subscriber( MessageBroker messageBroker, String queueName, String serviceName ) {
		if( messageBroker == null || queueName == null || serviceName == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		
		if( ! serviceName.matches( "^[a-zA-Z_][a-zA-Z_0-9]*$" ) ) {
			throw new RuntimeException( "Invalid serviceName" );
		}

		this.messageBroker = messageBroker;
		this.queue = messageBroker.messageQueue( queueName, serviceName );
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

	/**
	 * Defines the list of accepted sender roles for this subscriber. <br>
	 * Forces messages sent to this queue to have a valid JWT and makes sure that
	 * the list of roles included in the JWS (claim "roles") includes at least one
	 * of the accepted roles 
	 * 
	 * @throws RuntimeException if no {@link TokenFactory} was set prior to calling
	 *                          this method
	 */
	public void setAcceptedRoles( List<String> acceptedRoles ) {
		if( this.messageBroker.getTokenFactory() == null ) {
			throw new RuntimeException( "No token factory found" );
		}
		this.queue.setAcceptedRoles( acceptedRoles );
	}
}
