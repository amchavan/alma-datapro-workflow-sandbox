package alma.obops.draws.examples;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.security.TokenFactory;

/**
 * Example sender, sends a secured message. 
 */
@Component
public class SecureSender implements Runnable {

	@Autowired
	private MessageBroker broker;
	
	@Autowired
	@Qualifier( "jwt-token-factory" )
	private TokenFactory tokenFactory;

	@Override
	public void run() {
		broker.setTokenFactory( tokenFactory );
		MessageQueue queue = broker.messageQueue( BasicReceiver.QUEUE_NAME );
		BasicMessage freddie = new BasicMessage( "Freddie Mercury", 45, false );
		Envelope e = queue.send( freddie );
		System.out.println( ">>> Sent: " + e );
		System.exit( 0 );
	}
}
