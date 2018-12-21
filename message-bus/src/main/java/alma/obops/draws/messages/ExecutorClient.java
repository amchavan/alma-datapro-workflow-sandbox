package alma.obops.draws.messages;

import java.io.IOException;
import java.util.UUID;

/**
 * Implements a message-based RPC client (request/reply)
 * 
 * @author mchavan, 17-Sep-2018
 * @see Executor
 */
public class ExecutorClient {

	private MessageQueue queue;
	private MessageConsumer consumer;
	
	public static String makeResponseQueueName() {
		StringBuilder sb = new StringBuilder();
		sb.append( "response-queue-" )
		  .append( MessageBroker.nowISO() )
		  .append( "-" )
		  .append( UUID.randomUUID().toString().replace( "-", "" ) );
		
		return sb.toString();
	}
	
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
		
		// Create the queue for the Executor to publish its response
		String responseQueueName = makeResponseQueueName();		
		MessageQueue responseQueue = queue.getMessageBroker().messageQueue( responseQueueName );
		
		// Send the request
		request.setResponseQueueName( responseQueueName );
		queue.send( request );

		// Wait for an answer, delete the response queue when done -- response queues
		// are used only once
		Envelope response = responseQueue.receive( timeout );
		responseQueue.delete();		
		
		// Pass the response on to the consumer, and we're done
		this.consumer.consume( response.getMessage() );
	}
}
