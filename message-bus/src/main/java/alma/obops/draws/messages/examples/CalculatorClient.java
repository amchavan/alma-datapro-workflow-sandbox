package alma.obops.draws.messages.examples;

import static alma.obops.draws.messages.examples.ExampleUtils.*;
import static alma.obops.draws.messages.examples.Calculator.*;

import java.io.IOException;

import alma.obops.draws.messages.ExecutorClient;
import alma.obops.draws.messages.MessageBus;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.couchdb.CouchDbConfig;
import alma.obops.draws.messages.couchdb.CouchDbMessageBus;
import alma.obops.draws.messages.examples.Calculator.ComputationMessage;
import alma.obops.draws.messages.examples.Calculator.ResultMessage;

/**
 * Example client for the rudimentary server defined in {@link #Calculator}
 */
public class CalculatorClient {

	// This needs to be static to be visible inside the consumer lambda
	static ComputationMessage request;

	public static void main(String[] args) throws IOException {
		CouchDbConfig config = new CouchDbConfig();
		MessageBus bus = new CouchDbMessageBus(config, MESSAGE_BUS_NAME);
		MessageQueue queue = bus.messageQueue(CALC_SELECTOR);
		ExecutorClient client = new ExecutorClient(queue);

		MessageConsumer consumer = (message) -> {
			String txt = request.a + " " + request.operation + " " + request.b + " = "
					+ ((ResultMessage) message).value;
			System.out.println(txt);
		};

		request = new ComputationMessage("+", "1", "2");
		client.call(request, consumer);

		request = new ComputationMessage("-", "7", "4");
		client.call(request, consumer);

		request = new ComputationMessage("*", "1", "3");
		client.call(request, consumer);
	}
}
