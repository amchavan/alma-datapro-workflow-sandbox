package alma.obops.draws.examples;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import alma.obops.draws.messages.AbstractMessage;
import alma.obops.draws.messages.Executor;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.RequestMessage;
import alma.obops.draws.messages.RequestProcessor;
import alma.obops.draws.messages.ResponseMessage;
import alma.obops.draws.messages.TimeLimitExceededException;

/**
 * A trivial calculator, supports sum, subtraction and multiplication of
 * integers
 */
public class Calculator implements Runnable {

	public static final String CALC_SELECTOR = "compute";

	@Autowired
	private MessageBroker broker;

	/**
	 * A computation request message, e.g.
	 * <code>{"service":"sum", "a":"1", "b":"2"}</code>
	 */
	public static class ComputationMessage extends AbstractMessage implements RequestMessage {

		public String a;
		public String b;
		public String operation;

		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "[operation=" + operation + ",a=" + a + ",b=" + b + "]";
		}

		public ComputationMessage() {
			// empty
		}

		public ComputationMessage(String operation, String a, String b) {
			this.operation = operation;
			this.a = a;
			this.b = b;
		}
	}

	/**
	 * Describes a result, e.g. <code>{"value":"2"}</code><br>
	 * Our calculator responds with this kind of message
	 */
	public static class ResultMessage extends AbstractMessage implements ResponseMessage {
		public String value;

		public ResultMessage() {
		}

		public ResultMessage(int n) {
			value = String.valueOf(n);
		}

		@Override
		public String toString() {
			return "ResultMessage[value=" + value + "]";
		}
	}

	/** Implements our trivial calculator */
	public static class CalculatorProcessor implements RequestProcessor {

		@Override
		public ResponseMessage process(RequestMessage message) {

			System.out.println(">>> Received: " + message);
			ComputationMessage computation = (ComputationMessage) message;

			int result = 0;

			switch (computation.operation) {
			case "+":
				result = Integer.valueOf(computation.a) + Integer.valueOf(computation.b);
				break;

			case "-":
				result = Integer.valueOf(computation.a) - Integer.valueOf(computation.b);
				break;

			case "*":
				result = Integer.valueOf(computation.a) * Integer.valueOf(computation.b);
				break;

			default:
				throw new RuntimeException("Unsupported operation: '" + computation.operation + "'");
			}
			return new ResultMessage(result);
		}

	}

	@Override
	public void run() {
		MessageQueue queue = broker.messageQueue(CALC_SELECTOR);
		RequestProcessor processor = new CalculatorProcessor();
		Executor executor = new Executor(queue, processor, 5000);

		try {
			executor.run();
		} 
		catch( TimeLimitExceededException e ) {
			System.out.println(">>> Timed out: " + e.getMessage());
			System.exit( 0 );
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit( 1 );
		}
	}
}
