package alma.obops.draws.examples;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.security.TokenFactory;

/**
 * Example receiver, receives a secure message and rejects it if the sender does
 * not have the "OBOPS/AOD" role
 */
@Component
public class SecureReceiver implements Runnable {

	public static final String QUEUE_NAME = "BASIC";
	public static final String[] ACCEPTED_ROLES = { "OBOPS/AOD"/*, "QQQ"*/ };

	@Autowired
	@Qualifier( "rabbitmq-message-broker" )
	private MessageBroker broker;
	
	@Autowired
	@Qualifier( "jwt-token-factory" )
	private TokenFactory tokenFactory;
	
	@Override
	public void run() {
		broker.setTokenFactory( tokenFactory );
		
		MessageQueue queue = broker.messageQueue( QUEUE_NAME );
		queue.setAcceptedRoles( Arrays.asList( ACCEPTED_ROLES ));
		
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
