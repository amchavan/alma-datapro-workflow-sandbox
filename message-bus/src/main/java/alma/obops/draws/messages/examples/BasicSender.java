package alma.obops.draws.messages.examples;

import static alma.obops.draws.messages.examples.ExampleUtils.*;

import alma.obops.draws.messages.CouchDbMessageBus;
import alma.obops.draws.messages.MessageBus;
import alma.obops.draws.messages.MessageQueue;

/**
 * Example sender, sends a simple message. 
 */
public class BasicSender {

	public static final String QUEUE_NAME = "BASIC";

	public static void main( String[] args ) {
		
		MessageBus bus = new CouchDbMessageBus( COUCHDB_URL, COUCHDB_USERNAME, COUCHDB_PASSWORD, MESSAGE_BUS_NAME );	
		MessageQueue queue = bus.messageQueue( QUEUE_NAME );
		BasicMessage freddie = new BasicMessage( "Freddie Mercury", 45, false );
		queue.send( freddie );
		System.out.println( ">>> Sent: " + freddie );
	}
}
