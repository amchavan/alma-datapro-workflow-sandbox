package alma.obops.draws.messages;

/** 
 * An {@link Envelope} includes a {@link Message} providing metadata like forwarding information, timestamp, etc.
 */
public interface Envelope extends Record, Message {
	public String getCreationTimestamp() ;
	public Message getMessage();
	public String getMessageClass();
	public String getOriginIP();
	public String getQueueName();
	public void setMessage( Message message);
}
