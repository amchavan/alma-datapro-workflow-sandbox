package alma.obops.draws.messages;

import java.io.IOException;

/**
 * Implements a message-based RPC server (request/reply)
 * 
 * @author mchavan, 17-Sep-2018
 * @see ExecutorClient
 */
public class Executor {

	private Subscriber subscriber;
	private RequestProcessor processor;
	private int timeout;

	/**
	 * This {@link Executor}'s consumer: will invoke the processor on the given
	 * {@link RequestMessage} and send the result of that to a queue named after the
	 * original message's ID.
	 */
	private MessageConsumer consumer = (m) -> {
		
		// Compute the response
		RequestMessage message = (RequestMessage) m;
		Message response = processor.process( message );
		
		// Retrieve the response queue and publish the response there
		MessageBroker broker = subscriber.getQueue().getMessageBroker();
		Publisher publisher = new Publisher( broker, message.getResponseQueueName() );
		publisher.publish( response, 0 );
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
	public Executor( Subscriber subscriber, RequestProcessor processor, int timeout ) {
		this.subscriber = subscriber;
		this.processor = processor;
		this.timeout = timeout;
	}

	/**
	 * Listen to our message queue
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		subscriber.listen( consumer, timeout );
	}
}
