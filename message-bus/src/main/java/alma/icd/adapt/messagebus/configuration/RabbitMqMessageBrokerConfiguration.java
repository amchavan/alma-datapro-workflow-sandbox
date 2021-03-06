package alma.icd.adapt.messagebus.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import alma.icd.adapt.messagebus.MessageBroker;
import alma.icd.adapt.messagebus.rabbitmq.RabbitMqMessageBroker;

/**
 * Bean factory for our message broker.
 * 
 * @author amchavan, 14-Nov-2018
 */

@Configuration
public class RabbitMqMessageBrokerConfiguration {

	@Autowired
	RabbitMqConfigurationProperties rabbitMqProps;
	
	/**
	 * RabbitMQ broker, does <em>not</em> persist its messages; that's the default case.
	 */
	@Bean
	@Profile( "rabbitmq" )
	public MessageBroker rabbitMQ() {
		
		RabbitMqMessageBroker ret =
				new RabbitMqMessageBroker( rabbitMqProps.getConnection(), 
										   rabbitMqProps.getUsername(),
										   rabbitMqProps.getPassword(), 
										   null, 
										   null );
		return ret;
	}
}

