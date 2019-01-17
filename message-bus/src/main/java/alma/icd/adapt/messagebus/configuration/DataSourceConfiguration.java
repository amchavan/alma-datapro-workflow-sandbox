package alma.icd.adapt.messagebus.configuration;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Configure a JDBC DataSource configured according to ALMA conventions
 * 
 * @author amchavan, 18-Dec-2018
 */

@Configuration
@PropertySource("file:${ACSDATA}/config/archiveConfig.properties")
@Profile( "persisted-rabbitmq" )
public class DataSourceConfiguration {
	
	@Autowired
	Environment env;
	
	/** 
	 * @return A JDBC DataSource configured according to ALMA conventions, e.g.
	 *         retrieving the connection parameters from
	 *         <em>$ACSDATA/config/archiveConfig.properties</em>
	 */
	@Bean
	public DataSource confDataSource() {

		String url      = env.getProperty( "archive.relational.connection" );
		String username = env.getProperty( "archive.relational.user" );
		String password = env.getProperty( "archive.relational.passwd" );
		String driverClassName = env.getProperty("archive.relational.driverclassname");
		
		BasicDataSource ds = new BasicDataSource();
		ds.setUrl( url );
		ds.setUsername( username );
		ds.setPassword( password );
		if( driverClassName != null && !driverClassName.isEmpty()) {
		    ds.setDriverClassName(driverClassName);
		}
        ds.addConnectionProperty("v$session.program","draws");
		return ds;
	}
}

