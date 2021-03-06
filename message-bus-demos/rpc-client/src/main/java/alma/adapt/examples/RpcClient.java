package alma.adapt.examples;

import static alma.adapt.examples.common.Constants.DATETIME_QUEUE;
import static alma.adapt.examples.common.Utils.getCommandLineArg;
import static alma.icd.adapt.messagebus.MessageBroker.sleep;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.fasterxml.jackson.databind.ObjectMapper;

import alma.adapt.examples.common.DatetimeRequest;
import alma.adapt.examples.common.DatetimeResponse;
import alma.icd.adapt.messagebus.ExecutorClient;
import alma.icd.adapt.messagebus.MessageBroker;
import alma.icd.adapt.messagebus.MessageConsumer;
import alma.icd.adapt.messagebus.Publisher;

@SpringBootApplication
@ComponentScan( { "alma.icd.adapt.messagebus", "alma.adapt.examples" } )
public class RpcClient implements CommandLineRunner {
	
	private Logger logger = LoggerFactory.getLogger( RpcClient.class );

	@Autowired
	private MessageBroker broker;

	@Override
    public void run( String... args ) throws Exception {

		String repeatsArg = getCommandLineArg( "repeats", args );
		int repeats = 1;
		if( repeatsArg != null ) {
			repeats = Integer.parseInt( repeatsArg );
		}

		String delayArg = getCommandLineArg( "delay", args );
		int delay = 0;
		if( delayArg != null ) {
			delay = Integer.parseInt( delayArg );
		}

		Publisher publisher = new Publisher( broker, DATETIME_QUEUE );
		
		MessageConsumer consumer = (message) -> {
			logger.info( "Received reply: " + ((DatetimeResponse) message).datetime);
		};
		
		ExecutorClient client = new ExecutorClient( publisher, consumer );
		DatetimeRequest request = new DatetimeRequest( "Etc/GMT" );
		for( int i = 0; i < repeats; i++ ) {
			try {
				logger.info( "Sending request: " + new ObjectMapper().writeValueAsString( request ));
				client.call( request );
			} 
			catch (IOException e) {
				e.printStackTrace();
				System.exit( 1 );
			}
			sleep( delay * 1000 );
		}
		System.exit( 0 );
	}
	
	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication( RpcClient.class );
        app.setBannerMode( Banner.Mode.OFF );
        app.run( args );
	}
}
