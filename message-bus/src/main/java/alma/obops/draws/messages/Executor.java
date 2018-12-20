package alma.obops.draws.messages;

import java.io.IOException;

/**
 * Implements a message-based RPC server (request/reply)
 * 
 * @author mchavan, 17-Sep-2018
 * @see ExecutorClient
 */
public class Executor {

	private MessageQueue queue;
	private RequestProcessor processor;
	private int timeout;

	/**
	 * This {@link Executor}'s consumer: will invoke the processor on the given
	 * {@link RequestMessage} and send the result of that to a queue named after the
	 * original message's ID.
	 */
	private MessageConsumer consumer = (message) -> {
		Message response = processor.process( (RequestMessage) message );
		Envelope envelope = message.getEnvelope();
		
		// NOTE  We are creating here a temporary sending queue.
		MessageQueue responseQueue = queue.getMessageBroker().messageQueue( envelope.getId(),
				                                                            MessageQueue.Type.SENDQUEUE );
		System.out.println( ">>> server: sending to: " + responseQueue.getName() );
		queue.getMessageBroker().send( responseQueue, response, 0 );
		System.out.println( ">>> server: sent" );
		responseQueue.delete();
	};

	/**
	 * Public constructor
	 * 
	 * @param queue     Queue for input request messages
	 * @param processor Logic to process the requests
	 * @param timeout   If timeout > 0 it represents the number of msec to wait for
	 *                  a message to arrive before timing out -- upon timeout a
	 *                  RuntimeException is thrown
	 */
	public Executor( MessageQueue queue, RequestProcessor processor, int timeout ) {
		this.queue = queue;
		this.processor = processor;
		this.timeout = timeout;
	}

	/**
	 * Listen to our message queue
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		queue.listen( consumer, timeout );
	}
}
