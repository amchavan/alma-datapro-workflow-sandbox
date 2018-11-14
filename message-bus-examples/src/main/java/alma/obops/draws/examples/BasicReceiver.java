package alma.obops.draws.examples;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.TimeLimitExceededException;

/**
 * Example receiver, logs the message it receives (a single message)
 */
public class BasicReceiver implements Runnable {

	public static final String QUEUE_NAME = "BASIC";

	@Autowired
	@Qualifier( "rabbitmq-message-broker" )
	private MessageBroker broker;

	@Override
	public void run() {
		MessageQueue queue = broker.messageQueue( QUEUE_NAME );
		MessageConsumer consumer = (message) -> {
			System.out.println(">>> Received: " + message);
		};

		// Listen for a single message and pass it on to the consumer; will timeout
		// if no message can be read
		try {
			Envelope received = queue.receive( 10000 );
			consumer.consume( received.getMessage() );
		} 
		catch( IOException | TimeLimitExceededException e) {
			e.printStackTrace();
			System.exit( 1 );
		}

		System.exit( 0 );
	}
}
