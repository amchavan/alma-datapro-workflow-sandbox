package alma.obops.draws.messages.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Configure an in-memory JDBC DataSource
 * @author amchavan, 18-Dec-2018
 */

@Configuration
@Profile( {"unit-test-rabbitmq"} )
public class EmbeddedDataSourceConfiguration {

    @Autowired
    Environment env;
    
	/**
	 * @return An in-memory JDBC DataSource
	 */
	@Bean
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

