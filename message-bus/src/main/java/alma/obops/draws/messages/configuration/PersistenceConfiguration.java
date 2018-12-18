package alma.obops.draws.messages.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.JdbcConfiguration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJdbcRepositories
@Profile( "persisted-messages" )
public class PersistenceConfiguration extends JdbcConfiguration {

	@Autowired
	DataSource dataSource;
	
    @Bean
    NamedParameterJdbcOperations operations() { 
    	System.out.println( "1>>> " + dataSource );
        return new NamedParameterJdbcTemplate( dataSource );
    }

    @Bean
    PlatformTransactionManager transactionManager() { 
    	System.out.println( "2>>> " + dataSource );
        return new DataSourceTransactionManager( dataSource );
	}
}
