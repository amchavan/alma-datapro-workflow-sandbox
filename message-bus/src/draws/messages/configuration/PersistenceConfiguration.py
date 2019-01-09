class PersistenceConfiguration(JdbcConfiguration):
#    @Bean
#    NamedParameterJdbcOperations operations() { 
#        return new NamedParameterJdbcTemplate(dataSource());
#    }
#
#    @Bean
#    PlatformTransactionManager transactionManager() { 
#        return new DataSourceTransactionManager(dataSource());
#	}

    @Bean
    DataSource dataSource(){ 
        return new EmbeddedDatabaseBuilder()
                .setName( "obops" )
                .setType( EmbeddedDatabaseType.HSQL )
                .addScript( "rmq-message-broker-schema.sql" )
                .build();
    }
}
