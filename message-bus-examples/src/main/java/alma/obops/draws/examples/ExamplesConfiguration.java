package alma.obops.draws.examples;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author amchavan, 14-Nov-2018
 */

@Configuration
public class ExamplesConfiguration {

	@Bean
	public BasicSender bean0() {
		return new BasicSender();
	}
	
	@Bean
	public BasicReceiver bean1() {
		return new BasicReceiver();
	}
	
	@Bean
	public BasicExecutor bean2() {
		return new BasicExecutor();
	}

	@Bean
	public BasicExecutorClient bean3() {
		return new BasicExecutorClient();
	}
	
	@Bean
	public Calculator bean4() {
		return new Calculator();
	}
	
	@Bean
	public CalculatorClient bean5() {
		return new CalculatorClient();
	}
}

