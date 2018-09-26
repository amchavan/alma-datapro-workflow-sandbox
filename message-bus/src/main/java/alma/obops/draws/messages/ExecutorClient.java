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
	private MessageConsumer consumer;
	
	/**
	 * Public constructor
	 * 
	 * @param queue  Queue for sending request messages
	 */
	public ExecutorClient( MessageQueue queue, MessageConsumer consumer ) {
		this.queue = queue;
		this.consumer = consumer;
	}
	
	public void call( RequestMessage request ) throws IOException {
		call( request, 0 );
	}
	
	public void call( RequestMessage request, int timeout ) throws IOException {
		Envelope envelope = queue.send( request );
		MessageQueue responseQueue = queue.getMessageBus().messageQueue( envelope.getId() );
		responseQueue.listen( consumer, timeout, true );
	}
}
