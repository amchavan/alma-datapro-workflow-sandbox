package alma.obops.draws.messages;

import java.io.IOException;
import java.util.List;

import alma.obops.draws.messages.security.TokenFactory;

/**
 * Allows messages to be sent and subscribed to. Depends on a {@link MessageBroker}
 * providing the transport mechanism.
 * 
 * @author mchavan, 18-Sep-2018
 */
public class MessageQueue {
	
	/** Type of the queue */
	public enum Type {
		/** 
		 * The queue is used for sending messages, implementations may or may not
		 * create an underlying structure
         */
		SEND,
		
		/** 
		 * The queue is used for receiving messages
		 */
		RECEIVE, 
		
		/**
		 * The queue is used for sending messages, implementations should
		 * create an underlying structure
		 */
		SENDQUEUE
	}

	private String queueName;
	private MessageBroker messageBroker;
	private List<String> acceptedRoles;
	private Type type;	
	
	/**
	 * Create a {@link Type#RECEIVE} queue in the given broker.
	 * 
	 * @param name
	 *            If it ends with <code>.*</code> it is interpreted as a group ID;
	 *            messages set to the queue will be sent to all group members
	 */
	public MessageQueue( String queueName, MessageBroker messageBus ) {
		this( queueName, messageBus, Type.RECEIVE );
	}
	
	/**
	 * Create a queue in the given broker.
	 * 
	 * @param name
	 *            If it ends with <code>.*</code> it is interpreted as a group ID;
	 *            messages set to the queue will be sent to all group members
	 */
	public MessageQueue( String queueName, MessageBroker messageBus, Type type ) {
		if( queueName == null || messageBus == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		this.queueName = queueName;
		this.messageBroker = messageBus;
		this.type = type;
	}

	/**
	 * Delete this queue and all its messages
	 * 
	 * @throws IOException 
	 */
	public void delete() {
		messageBroker.deleteQueue( this );
	}

	/**
	 * @return The list of accepted sender roles for this queue. <br>
	 * Forces messages sent to this queue to have a valid JWT and makes sure that
	 * the list of roles included in the JWS (claim "roles") includes at least one
	 * of the accepted roles 
	 */
	public List<String> getAcceptedRoles() {
		return acceptedRoles;
	}
	
	public MessageBroker getMessageBroker() {
		return messageBroker;
	}
	
	public String getName() {
		return queueName;
	}

	public Type getType() {
		return type;
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
	 * Defines the list of accepted sender roles for this queue. <br>
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
		this.acceptedRoles = acceptedRoles;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + queueName + "]";
	}
}
