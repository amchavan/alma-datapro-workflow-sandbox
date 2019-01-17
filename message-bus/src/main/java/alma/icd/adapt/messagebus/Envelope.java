package alma.obops.draws.messages;

/**
 * An {@link Envelope} includes a {@link Message} providing metadata like
 * forwarding information, timestamp, etc.
 */
public interface Envelope {
	
	/**
	 * The state of this {@linkplain Envelope} -- or rather, of the enclosed
	 * {@linkplain Message}
	 */
	public enum State { Sent, Received, Consumed, Expired, Rejected }
	
	/**
	 * @return When the message was processed in some form by the receiver:
	 *         date/time string in ISO format, e.g. <code>2018-09-18T13:48:31</code>
	 */
	public String getConsumedTimestamp();
	
	/**
	 * @return When the message set to {@link State#Expired}: date/time string in
	 *         ISO format, e.g. <code>2018-09-18T13:48:31</code>
	 */
	public String getExpiredTimestamp();
	
	public String getId();
	
	/**
	 * @return The message we're carrying
	 */
	public Message getMessage();
	
	/** TODO */
	public String getMessageClass();
	
	/**
	 * @return The IP address of the host where this instance was generated
	 */
	public String getOriginIP();
	
	/**
	 * @return Name of the queue to which this message should be sent
	 */
	public String getQueueName();
	
	/**
	 * @return When the message was received (that is, read from the queue):
	 *         date/time string in ISO format, e.g. <code>2018-09-18T13:48:31</code>
	 */
	public String getReceivedTimestamp();
	
	/**
	 * @return When the message was sent: date/time string in ISO format, e.g.
	 *         <code>2018-09-18T13:48:31</code>
	 */
	public String getSentTimestamp();
	
	/**
	 * @return When the message was rejected: date/time string in ISO format, e.g.
	 *         <code>2018-09-18T13:48:31</code>
	 */
	public String getRejectedTimestamp();
	
	/**
	 * @return The current state of this instance
	 */
	public State getState();
	
	/**
	 * @return The time before this instance expires, in msec. If it's 0, this
	 *         instance has expired. If it's negative, this instance never
	 *         expires or has been read already.<br>
	 *         Only messages that haven't been received can expire.
	 */
	public long getTimeToLive();
	
	/**
	 * @return The authorization token sent with this instance, if any;
	 *         <code>null</code> otherwise
	 */
	public String getToken();
	
	/**
	 * @param message The message to carry
	 */
	public void setMessage( Message message );
}
