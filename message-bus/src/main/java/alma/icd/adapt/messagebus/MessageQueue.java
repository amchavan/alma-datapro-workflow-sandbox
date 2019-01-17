package alma.icd.adapt.messagebus;

import java.io.IOException;
import java.util.List;

import alma.icd.adapt.messagebus.security.TokenFactory;

/**
 * Allows messages to be subscribed to. Depends on a {@link MessageBroker}
 * providing the actual transport mechanism.
 * 
 * @author mchavan, 18-Sep-2018
 */
public class MessageQueue {

	private String queueName;
	private MessageBroker messageBroker;
	private List<String> acceptedRoles;
	private String serviceName;
	
	/**
	 * Create a queue in the given broker.
	 * <P>
	 * This constructor should never be called by client code, use
	 * {@link MessageBroker#messageQueue(String, String)} instead.
	 * 
	 * @param queueName   If it ends with <code>.*</code> it is interpreted as a
	 *                    group ID; messages set to the queue will be sent to all
	 *                    group members
	 * 
	 * @param serviceName Identifies the service (application) that's subscribing,
	 *                    as multiple services could subscribe to the same messages.
	 *                    <br>
	 *                    Must be a valid C/Python/Java variable name. <br>
	 *                    Must be unique system-wide.
	 */
	public MessageQueue( String queueName, String serviceName, MessageBroker messageBus ) {
		if( queueName == null || messageBus == null ) {
			throw new IllegalArgumentException( "Null arg" );
		}
		this.queueName = queueName;
		this.serviceName = serviceName;
		this.messageBroker = messageBus;
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
	 * Client code should never use this method (it's package-private anyway), use
	 * {@link Subscriber#setAcceptedRoles(List)} instead.
	 * 
	 * @return The list of accepted sender roles for this queue. <br>
	 *         Forces messages sent to this queue to have a valid JWT and makes sure
	 *         that the list of roles included in the JWS (claim "roles") includes
	 *         at least one of the accepted roles
	 */
	List<String> getAcceptedRoles() {
		return acceptedRoles;
	}

	public MessageBroker getMessageBroker() {
		return messageBroker;
	}
	
	public String getName() {
		return queueName;
	}
	
	public String getServiceName() {
		return serviceName;
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
	void setAcceptedRoles( List<String> acceptedRoles ) {
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
