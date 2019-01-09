package alma.obops.draws.examples;

import static alma.obops.draws.examples.common.Utils.getCommandLineArg;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageConsumer;
import alma.obops.draws.messages.Subscriber;
import alma.obops.draws.messages.TimeLimitExceededException;
import alma.obops.draws.messages.security.TokenFactory;

@SpringBootApplication
@ComponentScan( { "alma.obops.draws.messages", "alma.obops.draws.examples" } )
public class SecureReceiver implements CommandLineRunner {

	public static final String[] ACCEPTED_ROLES = { "OBOPS/AOD" };

	private Logger logger = LoggerFactory.getLogger( SecureReceiver.class );
	
	@Autowired
	private MessageBroker broker;
	
	@Autowired
	@Qualifier( "jwt-token-factory" )
	private TokenFactory tokenFactory;

	@Override
    public void run( String... args ) throws Exception {

		String serviceName = getCommandLineArg( "sname", args );
		if( serviceName == null ) {
			throw new IllegalArgumentException( "No service name command line argument 'sname'" );
		}
		
		String queueName = getCommandLineArg( "qname", args );
		if( queueName == null ) {
			throw new IllegalArgumentException( "No queue name command line argument 'qname'" );
		}

		Subscriber subscriber = new Subscriber( broker, queueName, serviceName );
		broker.setTokenFactory( tokenFactory );
		subscriber.setAcceptedRoles( Arrays.asList( ACCEPTED_ROLES ));
		
		MessageConsumer consumer = (message) -> {
			String pid = ManagementFactory.getRuntimeMXBean().getName();
			logger.info( serviceName + " (" + pid + ") received: " + message );
			logger.info( "    Token: " + message.getEnvelope().getToken() );
		};

		// Listen for a single message and pass it on to the consumer; will timeout
		// if no message can be read
		try {
			logger.info( "Waiting for message" );
			Envelope received = subscriber.receive( 120*1000 );
			consumer.consume( received.getMessage() );
		}
		catch( IOException e) {
			e.printStackTrace();
			System.exit( 1 );
		} 
		catch( TimeLimitExceededException e ) {
			logger.info( "Timing out: " + e.getMessage() );
		}
		
		broker.closeConnection();
	}
	
	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication( SecureReceiver.class );
        app.setBannerMode( Banner.Mode.OFF );
        app.run( args );
	}
}

