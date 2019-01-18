package alma.adapt.examples;

import static alma.adapt.examples.common.Utils.getCommandLineArg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import alma.adapt.examples.common.Person;
import alma.icd.adapt.messagebus.Envelope;
import alma.icd.adapt.messagebus.MessageBroker;
import alma.icd.adapt.messagebus.Publisher;

@SpringBootApplication
@ComponentScan( { "alma.icd.adapt.messagebus", "alma.adapt.examples" } )
public class Sender implements CommandLineRunner {
	
	private Logger logger = LoggerFactory.getLogger( Sender.class );

	@Autowired
	private MessageBroker broker;

	@Override
    public void run( String... args ) throws Exception {

		String queueName = getCommandLineArg( "qname", args );
		if( queueName == null ) {
			throw new IllegalArgumentException( "No queue name command line argument 'qname'" );
		}

		String repeatsArg = getCommandLineArg( "repeats", args );
		if( repeatsArg == null ) {
			repeatsArg = "1";
		}
		int repeats = Integer.parseInt( repeatsArg );

		String delayArg = getCommandLineArg( "delay", args );
		if( delayArg == null ) {
			delayArg = "0";
		}
		int delay = Integer.parseInt( delayArg );

		Publisher publisher = new Publisher( broker, queueName );
		logger.info( "Sending to " + queueName );
		
		/* 
		 * Main send loop
		 */
		for( int i = 0; i < repeats; i++ ) {
			Person p = new Person( "Person-"+i, 50+i, true );
			Envelope envelope = publisher.publish( p );
			logger.info( "Sent: " + envelope.getMessage() );
			Thread.sleep( delay * 1000 );
		}
		
		System.exit( 0 );
	}
	
	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication( Sender.class );
        app.setBannerMode( Banner.Mode.OFF );
        app.run( args );
	}
}

