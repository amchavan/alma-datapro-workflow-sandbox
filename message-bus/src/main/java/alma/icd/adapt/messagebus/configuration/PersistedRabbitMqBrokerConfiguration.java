package alma.icd.adapt.messagebus.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import alma.icd.adapt.messagebus.MessageBroker;
import alma.icd.adapt.messagebus.rabbitmq.RabbitMqMessageBroker;

/**
 * Bean factory for persisted message brokers.
 * 
 * @author amchavan, 14-Nov-2018
 */

@Configuration
@Profile( {"persisted-rabbitmq", "unit-test-rabbitmq"} )
public class PersistedRabbitMqBrokerConfiguration {

	@Autowired
	RabbitMqConfigurationProperties rabbitMqProps;

	@Autowired
	private PersistedEnvelopeRepository envelopeRepository;
	
	@Autowired
	private RecipientGroupRepository groupRepository;
	
	/**
	 * RabbitMQ broker
	 */
	@Bean
	public MessageBroker rabbitMQPersisted() {
		
		RabbitMqMessageBroker ret =
				new RabbitMqMessageBroker( rabbitMqProps.getConnection(), 
										   rabbitMqProps.getUsername(),
										   rabbitMqProps.getPassword(), 
										   envelopeRepository, 
										   groupRepository );
		return ret;
	}
}

