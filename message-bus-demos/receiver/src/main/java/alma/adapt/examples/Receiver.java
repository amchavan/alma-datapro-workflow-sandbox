package alma.adapt.examples;

import static alma.adapt.examples.common.Utils.getCommandLineArg;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import alma.icd.adapt.messagebus.MessageBroker;
import alma.icd.adapt.messagebus.MessageConsumer;
import alma.icd.adapt.messagebus.Subscriber;
import alma.icd.adapt.messagebus.TimeLimitExceededException;

@SpringBootApplication
@ComponentScan( { "alma.icd.adapt.messagebus", "alma.adapt.examples" } )
public class Receiver implements CommandLineRunner {
	
	private Logger logger = LoggerFactory.getLogger( Receiver.class );

	@Autowired
	private MessageBroker broker;

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
		
		MessageConsumer consumer = (message) -> {
			String pid = ManagementFactory.getRuntimeMXBean().getName();
			String msg = serviceName + " (" + pid + ") received: " + message;
			logger.info( msg );
		};

		// Listen for messages and pass them on to the consumer; will timeout
		// if no message is coming
		try {
			subscriber.listen( consumer, 120*1000 );
		} 
		catch( IOException e) {
			e.printStackTrace();
			System.exit( 1 );
		} 
		catch( TimeLimitExceededException e ) {
			logger.info( "Timing out: " + e.getMessage() );
			System.exit( 0 );
		}
		
		broker.closeConnection();
	}
	
	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication( Receiver.class );
        app.setBannerMode( Banner.Mode.OFF );
        app.run( args );
	}
}

