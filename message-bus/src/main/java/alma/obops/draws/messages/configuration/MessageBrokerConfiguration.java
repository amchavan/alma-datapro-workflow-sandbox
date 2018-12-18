package alma.obops.draws.messages.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.couchdb.CouchDbConnection;
import alma.obops.draws.messages.couchdb.CouchDbMessageBroker;
import alma.obops.draws.messages.rabbitmq.RabbitMqMessageBroker;

/**
 * Bean factory for our message broker.
 * 
 * @author amchavan, 14-Nov-2018
 */

@Configuration
public class MessageBrokerConfiguration {

	@Autowired
	RabbitMqConfigurationProperties rabbitMqProps;
	
	@Autowired
	CouchDbConnection couchDbConn;
	
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
	

	/**
	 * CouchDB broker
	 */
	@Bean
	@Profile( "couchdb" )
	public MessageBroker couchDB() {
		
		CouchDbMessageBroker ret = new CouchDbMessageBroker( couchDbConn );
		return ret;
	}
}

