package alma.obops.draws.messages.configuration;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Configure the application's data sources.
 * 
 * @author amchavan, 13-Nov-2018
 */

@Configuration
@PropertySource("file:${ACSDATA}/config/archiveConfig.properties")
public class DataSourceConfiguration {

	public static final String UNIT_TEST_PROFILE   = "unit-test-profile";
	public static final String PRODUCTION_PROFILE  = "production-profile";
	public static final String INTEGRATION_PROFILE = "integration-profile";
	
	@Autowired
	Environment env;
	
	/**
	 * This bean will be created only if the current active Spring profile
	 * is <em>not</em> {@value #UNIT_TEST_PROFILE}.
	 * 
	 * @return A JDBC DataSource configured according to ALMA conventions, e.g.
	 *         retrieving the connection parameters from
	 *         <em>$ACSDATA/config/archiveConfig.properties</em>
	 */
	@Bean( name = "production-data-source" )
	@Primary
	@Profile( PRODUCTION_PROFILE )
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
//        ds.addConnectionProperty("v$session.program","DASHboard");
		return ds;
	}
    
	/**
	 * This bean will be created only if the current active Spring profile is
	 * {@value #UNIT_TEST_PROFILE}.
	 * 
	 * @return A DataSource for an in-memory database
	 */
	@Bean( name="embedded-data-source" )
	@Profile( UNIT_TEST_PROFILE )
    public DataSource dataSource() {
		
	    EmbeddedDatabase testDataSrc=new EmbeddedDatabaseBuilder()
	        .setType( EmbeddedDatabaseType.H2 )
            .setName( "Dashboard embedded test database" )
            .addScript( "classpath:/rmq-message-broker-schema.sql" )
            .ignoreFailedDrops(true)
            .build();
	    return testDataSrc;
    }
}

