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

import alma.icd.adapt.messagebus.Envelope;
import alma.icd.adapt.messagebus.MessageBroker;
import alma.icd.adapt.messagebus.MessageConsumer;
import alma.icd.adapt.messagebus.Subscriber;
import alma.icd.adapt.messagebus.TimeLimitExceededException;

@SpringBootApplication
@ComponentScan( { "alma.icd.adapt.messagebus", "alma.adapt.examples" } )
public class BasicReceiver implements CommandLineRunner {

	private Logger logger = LoggerFactory.getLogger( BasicReceiver.class );
	
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
			String msg = ">>> Receiver " + serviceName + " (" + pid + ") received: " + message;
			logger.info( msg );
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
		
		SpringApplication app = new SpringApplication( BasicReceiver.class );
        app.setBannerMode( Banner.Mode.OFF );
        app.run( args );
	}
}

