package alma.obops.draws.messages;

import alma.obops.draws.messages.Envelope.State;

/**
 * Publishes messages to queue
 * 
 * @author mchavan, 07-Jan-2019
 */

public class Publisher {

	private MessageBroker messageBroker;
	private String messageAddress;

	/**
	 * @param messageBroker Our message broker
	 * @param queueName     Address of the messages we publish 
	 */
	public Publisher( MessageBroker messageBroker, String messageAddress ) {
		this.messageBroker = messageBroker;
		this.messageAddress = messageAddress;
	}

	public MessageBroker getMessageBroker() {
		return messageBroker;
	}
	
	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and publishes it to our queue. <br>
	 * The {@link Envelope} and {@link Message} instances reference each other.<br>
	 * The {@link Message} instance is set to {@link State#Sent}.
	 */
	public Envelope publish( Message message ) {
		return this.publish( message, 0 );
	} 	
	
	/**
	 * Creates an {@link Envelope} (including meta-data) from the given
	 * {@link Message} and publishes it to our queue. <br>
	 * The {@link Envelope} and {@link Message} instances reference each other.<br>
	 * The {@link Message} instance is set to {@link State#Sent}.
	 * 
	 * @param timeToLive
	 *            The time before this instance expires, in msec; if
	 *            <code>null</code>, this instance never expires
	 */
	public Envelope publish( Message message, long timeToLive ) {
		return messageBroker.send( messageAddress, message, timeToLive );
	}
}
