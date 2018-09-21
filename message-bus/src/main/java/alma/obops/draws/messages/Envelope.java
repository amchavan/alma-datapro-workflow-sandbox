package alma.obops.draws.messages;

public interface Envelope extends Record, Message {
	public String getCreationTimestamp() ;

	public Message getMessage();
	public String getMessageClass();
	public String getOriginIP();
	public String getQueueName();
	public void setMessage( Message message);
}
