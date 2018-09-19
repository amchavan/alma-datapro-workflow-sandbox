package alma.obops.draws.messages;

import java.io.IOException;

/**
 * Implements a message-based RPC client (request/reply)
 * 
 * @author mchavan, 17-Sep-2018
 * @see Executor
 */
public class ExecutorClient {

	private MessageQueue queue;
	
	/**
	 * Public constructor
	 * 
	 * @param queue  Queue for sending request messages
	 */
	public ExecutorClient( MessageQueue queue ) {
		this.queue = queue;
	}
	
	public void call( RequestMessage request, MessageConsumer consumer ) throws IOException {
		call( request, consumer, 0 );
	}
	
	public void call( RequestMessage request, MessageConsumer consumer, int timeout ) throws IOException {
		Envelope envelope = queue.send( request );
		MessageQueue responseQueue = queue.getMessageBus().messageQueue( envelope.getId() );
		responseQueue.listen( consumer, timeout, false, true );
	}
}
