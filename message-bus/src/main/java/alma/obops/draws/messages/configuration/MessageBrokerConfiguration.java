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
 * Bean factory for the message brokers.
 * 
 * @author amchavan, 14-Nov-2018
 */

@Configuration
public class MessageBrokerConfiguration {

	@Autowired
	RabbitMqConfigurationProperties rabbitMqProps;
	
	@Autowired
	CouchDbConnection couchDbConn;

	@Autowired
	private PersistedEnvelopeRepository envelopeRepository;
	
	@Autowired
	private RecipientGroupRepository groupRepository;
	
	/**
	 * RabbitMQ broker
	 */
	@Bean//( name = "rabbitmq-message-broker" )
	@Profile( "rabbitmq" )
	public MessageBroker rabbitMQ() {
		
		RabbitMqMessageBroker ret =
				new RabbitMqMessageBroker( rabbitMqProps.getConnection(), 
										   rabbitMqProps.getUsername(),
										   rabbitMqProps.getPassword(), 
										   envelopeRepository, 
										   groupRepository );
		return ret;
	}
	

	/**
	 * CouchDB broker
	 */
	@Bean( name = "couchdb-message-broker" )
	@Profile( "couchdb" )
	public MessageBroker couchDB() {
		
		CouchDbMessageBroker ret = new CouchDbMessageBroker( couchDbConn );
		return ret;
	}
}

