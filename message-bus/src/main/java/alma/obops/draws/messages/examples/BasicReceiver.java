package alma.obops.draws.messages.examples;

import static alma.obops.draws.messages.examples.ExampleUtils.*;

import alma.obops.draws.messages.Envelope;

import static alma.obops.draws.messages.examples.BasicSender.*;

import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.couchdb.CouchDbConfig;
import alma.obops.draws.messages.couchdb.CouchDbMessageBroker;

/**
 * Example receiver, logs the message it receives (a single message)
 */
public class BasicReceiver {

	public static void main(String[] args) throws Exception {
		CouchDbConfig config = new CouchDbConfig();
		MessageBroker bus = new CouchDbMessageBroker(config, MESSAGE_BUS_NAME);
		MessageQueue queue = bus.messageQueue(QUEUE_NAME);
		MessageConsumer consumer = (message) -> {
			System.out.println(">>> Received: " + message);
		};

		// Listen for a single message and pass it on to the consumer; will timeout
		// if no message can be read
		Envelope received = queue.receive( 10000 );
		consumer.consume( received.getMessage() );
	}
}
