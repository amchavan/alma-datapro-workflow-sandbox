package alma.obops.draws.examples;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import alma.obops.draws.messages.MessageBroker;

@SpringBootApplication
@ComponentScan( { "alma.obops.draws.messages", "alma.obops.draws.examples" } )
public class Application implements CommandLineRunner {

	@Autowired
	@Qualifier( "rabbitmq-message-broker" )
	private MessageBroker broker;

	@Autowired
	private BasicSender basicSender;
	
	@Autowired
	private SecureSender secureSender;
	
	@Autowired
	private BasicReceiver basicReceiver;
	
	@Autowired
	private SecureReceiver secureReceiver;
	
	@Autowired
	private BasicExecutor basicExecutor;
	
	@Autowired
	private BasicExecutorClient basicExecutorClient;
	
	@Autowired
	private Calculator calculator;
	
	@Autowired
	private CalculatorClient calculatorClient;
	
	
	
	/**
	 * Usage<br>
	 * <code>java -jar .....jar &lt;example&gt;</code><br>
	 * where <i>example</i> is the simple class name of one of the examples, for
	 * instance {@link BasicSender}.
	 */
	@Override
    public void run( String... args ) throws Exception {
		
		String target = null;
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if( arg == null || arg.length() == 0 ) {
				// is this even possible???
				continue;
			}
			if( arg.startsWith( "-" )) {
				// skip command line options
				continue;			
			}
			target = arg;
			break;
		}
		
		if( target == null ) {
			throw new RuntimeException( "No example classname given on the command line" );
		}
		
		switch( target ) {
		
		case "BasicSender":
			basicSender.run();
			break;

		case "BasicReceiver":
			basicReceiver.run();
			break;

		case "BasicExecutor":
			basicExecutor.run();
			break;

		case "BasicExecutorClient":
			basicExecutorClient.run();
			break;

		case "Calculator":
			calculator.run();
			break;

		case "CalculatorClient":
			calculatorClient.run();
			break;

		case "SecureSender":
			secureSender.run();
			break;

		case "SecureReceiver":
			secureReceiver.run();
			break;
			
		default:
			throw new RuntimeException( "Unsupported target: " + target );
		}
    }
	
	
	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication( Application.class );
        app.setBannerMode( Banner.Mode.OFF );
        app.run( args );
	}
}
