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

	private Publisher publisher;
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
	public ExecutorClient( Publisher publisher, MessageConsumer consumer ) {
		this.publisher = publisher;
		this.consumer = consumer;
	}
	
	public void call( RequestMessage request ) throws IOException {
		call( request, 0 );
	}
	
	public void call( RequestMessage request, int timeout ) throws IOException {
		
		// Create the queue for the Executor to publish its response
		String responseQueueName = makeResponseQueueName();	
		Subscriber subscriber = new Subscriber( publisher.getMessageBroker(), 
												responseQueueName, 
												"temp" );
		
		// publish the request
		request.setResponseQueueName( responseQueueName );
		publisher.publish( request );

		// Wait for an answer, delete the response queue when done -- response queues
		// are used only once
		Envelope response = subscriber.receive( timeout );
		subscriber.getQueue().delete();		
		
		// Pass the response on to the consumer, and we're done
		this.consumer.consume( response.getMessage() );
	}
}
