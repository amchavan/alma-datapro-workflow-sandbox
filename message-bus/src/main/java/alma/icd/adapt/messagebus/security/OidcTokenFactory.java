package alma.icd.adapt.messagebus.security;

import static alma.icd.adapt.messagebus.HttpUtils.httpGetMap;
import static alma.icd.adapt.messagebus.HttpUtils.httpGetString;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

/**
 * An implementation of the {@link TokenFactory} interface that's specific
 * to the CAS/OAth2/OIDC server.<br>
 * Tokens are retrieved via the <i>Client Credentials</i> grant, e.g. with a 
 * GET to 
 * <code>https://&lt;host>:&lt;port>/cas/oidc/token? response_type=id_token%20token & grant_type=password & 
 * 		    client_id=&lt;client-id> & 
 *          username=&lt;username> & 
 *          password=&lt;password></code>
 * @author mchavan, 13-Dec-2018
 */
@Component
public class OidcTokenFactory implements TokenFactory {
	
	private static final String NO_SECURITY_CHECKS_MSG = "Invalid OIDC service properties \"obops.oidc\", NO SECURITY CHECKS";

	protected static OidcTokenFactory instance = null;
	
	Logger logger = LoggerFactory.getLogger( OidcTokenFactory.class );

	private String oidcResourceOwnerGrantURL = null;
	private boolean skipAll = false;
	
	private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
	
	public OidcTokenFactory( OidcConfigurationProperties properties ) {
		
		String oidcServerUrl = properties.getServerUrl();
		String oidcServiceId = properties.getServiceId();
		String oidcResourceOwnerId = properties.getResourceOwnerId();
		String oidcResourceOwnerPassword = properties.getResourceOwnerPassword();
	
		if( isEmpty( oidcServerUrl ) ||
			isEmpty( oidcServiceId ) ||
			isEmpty( oidcResourceOwnerId ) ||
			isEmpty( oidcResourceOwnerPassword )) {
			
			logger.warn( NO_SECURITY_CHECKS_MSG );
			skipAll = true;
			return;
		}
				
		// OIDC server URL, let's be lenient about the trailing slash
		if( ! oidcServerUrl.endsWith( "/" )) {
			oidcServerUrl += "/";
		}
		
		this.oidcResourceOwnerGrantURL = 
				oidcServerUrl 
				+ "oidc/token?response_type=id_token%20token&grant_type=password&" 
				+ "client_id=" + oidcServiceId + "&"
				+ "username=" + oidcResourceOwnerId + "&"
				+ "password=" + oidcResourceOwnerPassword;
		
		// Retrieve this OIDC authentication server's JWK set
		// and use it to create a JWT processor
		// See https://connect2id.com/products/nimbus-jose-jwt/examples/validating-jwt-access-tokens
		
		String oidcPublicKeysURL = oidcServerUrl + "/oidc/jwks";
		this.jwtProcessor = new DefaultJWTProcessor<SecurityContext>();
		try {
			String json = httpGetString( oidcPublicKeysURL );			
			JWKSet jwkSet = JWKSet.parse( json );
			JWKSource<SecurityContext> keySource = new ImmutableJWKSet<SecurityContext>( jwkSet );
			JWSAlgorithm alg = JWSAlgorithm.RS256;
			JWSKeySelector<SecurityContext> sel  = new JWSVerificationKeySelector<SecurityContext>( alg, keySource );
			jwtProcessor.setJWSKeySelector( sel );
		} 
		catch( Exception e ) {
			logger.error( e.getMessage(), e );
			throw new RuntimeException( e );
		}
	}

	@Override
	public String create() {
		
		if( skipAll ) {			
			logger.warn( NO_SECURITY_CHECKS_MSG );
			return "";
		}
		
		try {
			Map<String, String> body = httpGetMap( this.oidcResourceOwnerGrantURL );
			return body.get( "id_token" );
		} 
		catch( Exception e ) {
			logger.error( e.getMessage(), e );
			throw new RuntimeException( e );
		}
	}

	@Override
	public String create(Map<String, Object> claims) {
		throw new RuntimeException( "Not implemented" );
	}
	
	private boolean isEmpty( String s ) {
		return (s == null) || (s.length() == 0);
	}

	@Override
	public boolean isValid( String token ) {
		try {
			decode( token );
			return true;
		}
		catch( Exception e ) {
			return false;
		}
	}

	@Override
	public Map<String, Object> decode( String token ) {
		try {
			SecurityContext ctx = null; // optional context parameter, not required here
			JWTClaimsSet claimsSet = jwtProcessor.process( token, ctx );
			return claimsSet.getClaims();
		} 
		catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}
}