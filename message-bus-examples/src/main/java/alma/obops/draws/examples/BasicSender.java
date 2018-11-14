package alma.obops.draws.examples;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageQueue;

/**
 * Example sender, sends a simple message. 
 */
@Component
public class BasicSender implements Runnable {

	@Autowired
	@Qualifier( "rabbitmq-message-broker" )
	private MessageBroker broker;

	@Override
	public void run() {
		MessageQueue queue = broker.messageQueue( BasicReceiver.QUEUE_NAME );
		BasicMessage freddie = new BasicMessage( "Freddie Mercury", 45, false );
		queue.send( freddie );
		System.out.println( ">>> Sent: " + freddie );
		System.exit( 0 );
	}
}
