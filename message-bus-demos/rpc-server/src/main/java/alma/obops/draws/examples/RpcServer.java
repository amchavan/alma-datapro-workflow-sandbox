package alma.obops.draws.examples;

import static alma.obops.draws.examples.common.Constants.DATETIME_QUEUE;

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

import alma.obops.draws.examples.common.DatetimeRequest;
import alma.obops.draws.examples.common.DatetimeResponse;
import alma.obops.draws.messages.Executor;
import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.MessageQueue;
import alma.obops.draws.messages.RequestMessage;
import alma.obops.draws.messages.RequestProcessor;
import alma.obops.draws.messages.ResponseMessage;
import alma.obops.draws.messages.TimeLimitExceededException;

@SpringBootApplication
@ComponentScan( { "alma.obops.draws.messages", "alma.obops.draws.examples" } )
public class RpcServer implements CommandLineRunner {
	
	private Logger logger = LoggerFactory.getLogger( RpcServer.class );

	@Autowired
	private MessageBroker broker;

	@Override
    public void run( String... args ) throws Exception {
		
		broker.setServiceName( "rpc_server" );
		MessageQueue queue = broker.messageQueue( DATETIME_QUEUE );
		RequestProcessor processor = new DatetimeProcessor();
		Executor executor = new Executor( queue, processor, 120*1000 );

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

	@Override
	public ResponseMessage process( RequestMessage message ) {

		DatetimeRequest request = (DatetimeRequest) message;
		System.out.println(">>> Received request with TZ=" + request.timezone);
		TimeZone tz = TimeZone.getTimeZone(request.timezone);
		Calendar c = Calendar.getInstance(tz);
		return new DatetimeResponse( c.getTime().toString() );
	}
}
