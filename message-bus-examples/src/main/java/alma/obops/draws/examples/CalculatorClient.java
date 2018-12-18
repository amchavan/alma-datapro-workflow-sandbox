package alma.obops.draws.examples;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import alma.obops.draws.examples.Calculator.ComputationMessage;
import alma.obops.draws.examples.Calculator.ResultMessage;
import alma.obops.draws.messages.ExecutorClient;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;

/**
 * Example client for the rudimentary server defined in {@link #Calculator}
 */
public class CalculatorClient implements Runnable {

	@Autowired
	private MessageBroker broker;

	// This needs to be static to be visible inside the consumer lambda
	static ComputationMessage request;

	@Override
	public void run() {
		MessageQueue queue = broker.messageQueue( Calculator.CALC_SELECTOR );

		MessageConsumer consumer = (message) -> {
			String txt = request.a + " " + request.operation + " " + request.b + " = "
					+ ((ResultMessage) message).value;
			System.out.println(txt);
		};
		ExecutorClient client = new ExecutorClient(queue, consumer);

		try {
			request = new ComputationMessage("+", "1", "2");
			client.call(request);

			request = new ComputationMessage("-", "7", "4");
			client.call(request);

			request = new ComputationMessage("*", "1", "3");
			client.call(request);
		} 
		catch (IOException e) {
			e.printStackTrace();
			System.exit( 1 );
		}
		
		System.exit( 0 );
	}
}
