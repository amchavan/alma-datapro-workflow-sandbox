package alma.obops.draws.messages.examples;

import static alma.obops.draws.messages.examples.ExampleUtils.*;
import static alma.obops.draws.messages.examples.BasicExecutor.*;

import java.io.IOException;

import alma.obops.draws.messages.ExecutorClient;
import alma.obops.draws.messages.MessageBus;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.couchdb.CouchDbConfig;
import alma.obops.draws.messages.couchdb.CouchDbMessageBus;

/**
 * Example client for basic executor defined in {@link #BasicExecutor}
 */
public class BasicExecutorClient {

	public static void main(String[] args) throws IOException {
		CouchDbConfig config = new CouchDbConfig();
		MessageBus bus = new CouchDbMessageBus(config, MESSAGE_BUS_NAME);
		MessageQueue queue = bus.messageQueue(DATETIME_QUEUE);
		MessageConsumer consumer = (message) -> {
			System.out.println(((DatetimeResponse) message).datetime);
		};
		ExecutorClient client = new ExecutorClient( queue, consumer );

		DatetimeRequest request = new DatetimeRequest("Etc/GMT");
		client.call( request );
	}
}
