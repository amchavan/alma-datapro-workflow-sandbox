package alma.obops.draws.messages.examples;

import static alma.obops.draws.messages.examples.ExampleUtils.*;
import static alma.obops.draws.messages.examples.BasicExecutor.*;

import java.io.IOException;

import alma.obops.draws.messages.CouchDbMessageBus;
import alma.obops.draws.messages.ExecutorClient;
import alma.obops.draws.messages.MessageBus;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;

/**
 * Example client for basic executor defined in {@link #BasicExecutor}
 */
public class BasicExecutorClient {
	
	public static void main(String[] args) throws IOException {		
		
		MessageBus messageBus = new CouchDbMessageBus( COUCHDB_URL, COUCHDB_USERNAME, COUCHDB_PASSWORD, MESSAGE_BUS_NAME );
		MessageQueue queue = messageBus.messageQueue( DATETIME_QUEUE );
		ExecutorClient client = new ExecutorClient( queue );		
		DatetimeRequest request = new DatetimeRequest( "Etc/GMT" );
		MessageConsumer consumer = (message) -> {
			System.out.println( ((DatetimeResponse) message).datetime );
		};
		
		client.call( request, consumer );
	}
}
