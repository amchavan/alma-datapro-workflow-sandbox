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
	private boolean justOne;

	/**
	 * This {@link Executor}'s consumer: will invoke the processor on the given
	 * {@link RequestMessage} and send the result of that to a queue named after the
	 * original message's ID.
	 */
	private MessageConsumer consumer = (message) -> {
		Message response = processor.process( (RequestMessage) message );
		Envelope envelope = message.getEnvelope();
		MessageQueue responseQueue = queue.getMessageBroker().messageQueue( envelope.getId() );
		queue.getMessageBroker().sendOne( responseQueue, response, 0 );
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
	public Executor(MessageQueue queue, RequestProcessor processor, int timeout) {
		this(queue, processor, timeout, false);
	}

	/**
	 * Public constructor
	 * 
	 * @param queue     Queue for input request messages
	 * @param processor Logic to process the requests
	 * @param timeout   If timeout > 0 it represents the number of msec to wait for
	 *                  a message to arrive before timing out -- upon timeout a
	 *                  RuntimeException is thrown
	 * @param justOne   If <code>true</code>, terminate after processing the first
	 *                  message
	 */
	public Executor(MessageQueue queue, RequestProcessor processor, int timeout, boolean justOne) {
		this.queue = queue;
		this.processor = processor;
		this.timeout = timeout;
		this.justOne = justOne;
	}

	/**
	 * Listen to our message queue
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		queue.listen( consumer, timeout,  justOne );
	}
}
