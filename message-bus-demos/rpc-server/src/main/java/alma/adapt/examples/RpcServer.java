package alma.adapt.examples;

import static alma.adapt.examples.common.Constants.DATETIME_QUEUE;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import alma.adapt.examples.common.DatetimeRequest;
import alma.adapt.examples.common.DatetimeResponse;
import alma.icd.adapt.messagebus.Executor;
import alma.icd.adapt.messagebus.MessageBroker;
import alma.icd.adapt.messagebus.RequestMessage;
import alma.icd.adapt.messagebus.RequestProcessor;
import alma.icd.adapt.messagebus.ResponseMessage;
import alma.icd.adapt.messagebus.Subscriber;
import alma.icd.adapt.messagebus.TimeLimitExceededException;

@SpringBootApplication
@ComponentScan( { "alma.icd.adapt.messagebus", "alma.adapt.examples" } )
public class RpcServer implements CommandLineRunner {
	
	private Logger logger = LoggerFactory.getLogger( RpcServer.class );

	@Autowired
	private MessageBroker broker;

	@Override
    public void run( String... args ) throws Exception {

		Subscriber subscriber = new Subscriber( broker, DATETIME_QUEUE, "rpc_server" );
		RequestProcessor processor = new DatetimeProcessor();
		Executor executor = new Executor( subscriber, processor, 120*1000 );

		try {
			executor.run();
		} 
		catch( TimeLimitExceededException e ) {
			logger.info( "Timed out: " + e.getMessage() );
			System.exit( 0 );
		} 
		catch (IOException e) {
			e.printStackTrace();
			System.exit( 1 );
		}
	}
	
	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication( RpcServer.class );
        app.setBannerMode( Banner.Mode.OFF );
        app.run( args );
	}
}

/** Implements this service's logic */
class DatetimeProcessor implements RequestProcessor {
	
	private Logger logger = LoggerFactory.getLogger( DatetimeProcessor.class );

	@Override
	public ResponseMessage process( RequestMessage message ) {

		DatetimeRequest request = (DatetimeRequest) message;
		logger.info(">>> Received request with TZ=" + request.timezone);
		TimeZone tz = TimeZone.getTimeZone(request.timezone);
		Calendar c = Calendar.getInstance(tz);
		return new DatetimeResponse( c.getTime().toString() );
	}
}
