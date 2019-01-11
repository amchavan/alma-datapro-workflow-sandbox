package alma.obops.draws.dashboard.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure a CouchDB connection from the ALMA properties file
 * 
 * @author amchavan, 13-Nov-2018
 */

@Configuration
public class CouchDbConfiguration {
	
	@Autowired
	CouchDbConfigurationProperties properties;
	
	/**
	 * @return A {@link CouchDbConnection} configured according to ALMA conventions, e.g.
	 *         retrieving the connection parameters from
	 *         <em>$ACSDATA/config/archiveConfig.properties</em>
	 */

	@Bean( name="couch-db-connection" )
	public CouchDbConnection couchDbConnection() {

		String url      = properties.getConnection();
		String username = properties.getUsername();
		String password = properties.getPassword();
		
		CouchDbConnection cdbConn = new CouchDbConnection( url, username, password );
		return cdbConn;
	}
}

