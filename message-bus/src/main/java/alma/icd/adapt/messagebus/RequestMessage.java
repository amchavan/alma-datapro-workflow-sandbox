package alma.obops.draws.messages;

/**
 * All messages sent as a request by an RPC (request/reply) client must implement
 * this interface.
 * 
 * @author mchavan, 17-Sep-2018
 */
public interface RequestMessage extends Message {
	
	/**
	 * @return Name of the queue for the server to publish its response
	 */
	public String getResponseQueueName();
	
	/**
	 * @param name Name of the queue for the server to publish its response
	 */
	public void setResponseQueueName( String name );
}
