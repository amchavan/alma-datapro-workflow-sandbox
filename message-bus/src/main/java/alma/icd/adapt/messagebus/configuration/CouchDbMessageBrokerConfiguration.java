package alma.obops.draws.messages.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import alma.obops.draws.messages.MessageBroker;
import alma.obops.draws.messages.couchdb.CouchDbConnection;
import alma.obops.draws.messages.couchdb.CouchDbMessageBroker;

/**
 * Bean factory for our message broker.
 * 
 * @author amchavan, 14-Nov-2018
 */

@Configuration
public class CouchDbMessageBrokerConfiguration {
	
	@Autowired
	CouchDbConnection couchDbConn;

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

