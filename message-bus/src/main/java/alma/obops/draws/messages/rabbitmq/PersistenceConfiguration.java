package alma.obops.draws.messages.rabbitmq;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.JdbcConfiguration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJdbcRepositories
public class PersistenceConfiguration extends JdbcConfiguration {

    @Bean
    NamedParameterJdbcOperations operations() { 
        return new NamedParameterJdbcTemplate(dataSource());
    }

    @Bean
    PlatformTransactionManager transactionManager() { 
        return new DataSourceTransactionManager(dataSource());
	}

    @Bean
    DataSource dataSource(){ 
        return new EmbeddedDatabaseBuilder()
                .setName( "obops" )
                .setType( EmbeddedDatabaseType.HSQL )
                .addScript( "rmq-message-broker-schema.sql" )
                .build();
    }
}
