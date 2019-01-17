package alma.obops.draws.messages.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import alma.obops.draws.messages.security.JWTFactory;
import alma.obops.draws.messages.security.OidcConfigurationProperties;
import alma.obops.draws.messages.security.OidcTokenFactory;
import alma.obops.draws.messages.security.TokenFactory;

/**
 * Bean factory for the message brokers.
 * 
 * @author amchavan, 14-Nov-2018
 */

@Configuration
public class TokenFactoryConfiguration {

	@Autowired
	OidcConfigurationProperties oidcProps;
	
	/**
	 * OIDC token factory
	 */
	@Bean( name = "oidc-token-factory" )
	public TokenFactory oidc() {
		OidcTokenFactory ret = new OidcTokenFactory( oidcProps );
		return ret;
	}
	

	/**
	 * JWT token factory -- for testing only, DO NOT USE IN PRODUCTION
	 */
	@Bean( name = "jwt-token-factory" )
	public TokenFactory jwt() {
		JWTFactory ret = new JWTFactory();
		return ret;
	}
}

