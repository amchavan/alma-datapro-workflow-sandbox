package alma.obops.draws.examples;

import static alma.obops.draws.examples.common.Utils.getCommandLineArg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import alma.obops.draws.examples.common.Person;
import alma.obops.draws.messages.Envelope;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.Publisher;

@SpringBootApplication
@ComponentScan( { "alma.obops.draws.messages", "alma.obops.draws.examples" } )
public class BasicSender implements CommandLineRunner {
	
	private Logger logger = LoggerFactory.getLogger( BasicSender.class );

	@Autowired
	private MessageBroker broker;

	@Override
    public void run( String... args ) throws Exception {

		String queueName = getCommandLineArg( "qname", args );
		if( queueName == null ) {
			throw new IllegalArgumentException( "No queue name command line argument 'qname'" );
		}
		
		Publisher publisher = new Publisher( broker, queueName );
		Person freddie = new Person( "Freddie Mercury", 45, false );
		logger.info( "Sending to " + queueName );
		Envelope envelope = publisher.publish( freddie );
		System.out.println( ">>> Sent: " + envelope.getMessage() );
		System.exit( 0 );
	}
	
	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication( BasicSender.class );
        app.setBannerMode( Banner.Mode.OFF );
        app.run( args );
	}
}

