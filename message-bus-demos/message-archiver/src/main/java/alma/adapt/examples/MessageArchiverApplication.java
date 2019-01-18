package alma.adapt.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import alma.icd.adapt.messagebus.MessageBroker;
import alma.icd.adapt.messagebus.rabbitmq.RabbitMqMessageBroker;

@SpringBootApplication
@ComponentScan( { "alma.icd.adapt", "alma.adapt" } )
public class MessageArchiverApplication implements CommandLineRunner {
	
	private Logger logger = LoggerFactory.getLogger( MessageArchiverApplication.class );

	@Autowired
	private MessageBroker injectedBroker;

	@Override
    public void run( String... args ) throws Exception {
		
		RabbitMqMessageBroker broker = (RabbitMqMessageBroker) injectedBroker;
		Runnable messageArchiver = broker.getMessageArchiver();
		Thread messageArchiverThread = new Thread( messageArchiver );
		
		logger.info( "Starting archiver thread" );
		messageArchiverThread.start();
//		broker.closeConnection();
	}
	
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication( MessageArchiverApplication.class );
        app.setBannerMode( Banner.Mode.OFF );
        app.run( args );
	}
}

