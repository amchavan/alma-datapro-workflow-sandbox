package alma.obops.draws.messages.security;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;

import net.minidev.json.JSONObject;

/**
 * See https://connect2id.com/products/nimbus-jose-jwt/examples/jws-with-rsa-signature
 * @author mchavan, 14-Dec-2018 -- rewritten with JOSE
 */
public class JWTFactory implements TokenFactory {

	// Private and public keys. This should be in a property file, not here.
	// Generated with https://mkjwk.org/
    private static final String JWKS = "{\"kty\":\"RSA\",\"d\":\"g_JYrlcTrwMPWM0ONWdJsw9f6jQoyNZBAdt8CPcSQmZA4iuBnX2LwIih3NgunBWA1TfERAoyD3mRiPU-yBidgQ\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"testkey\",\"alg\":\"RS256\",\"n\":\"hDLlR76CTTQjnRyA0NqpBDcdq_Nc3fqPNXplLXUf9PSbolxm_SyYfUiTO2NhHy74Z13nZgWwpkJiM7K0QdlU5w\"}";
	private static final String TIME_TO_LIVE_CLAIM = "ttl";

    private RSAKey rsaJWK;
	private RSAKey rsaPublicJWK;
	private JWSSigner signer;

	
	public JWTFactory() throws ParseException, JOSEException {
		this.rsaJWK = RSAKey.parse( JWKS );
		this.rsaPublicJWK = rsaJWK.toPublicJWK();
		this.signer = new RSASSASigner(rsaJWK);
	}

	@Override
	public String create() {
		Map<String, Object> claims = new HashMap<>();
		claims.put( "sub", "user" );
		claims.put( "role", "admin" );
		claims.put( TIME_TO_LIVE_CLAIM, 10000L );
		return create( claims );
	}

	@Override
	public String create( Map<String, Object> claims ) {
		
		try {
			JSONObject jose = new JSONObject( claims );
			Payload payload = new Payload( jose );
			JWSHeader builder = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build();
			JWSObject jwsObject = new JWSObject( builder, payload );
			jwsObject.sign( signer );
			String ret = jwsObject.serialize();
			return ret;
		} 
		catch( JOSEException e ) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public Map<String,Object> decode( String jwt ) {	
		try {
			Map<String,Object> ret = new HashMap<>();
			JWSObject jwsObject = JWSObject.parse( jwt );
			final JSONObject jsonObject = jwsObject.getPayload().toJSONObject();
			Set<String> claimNames = jsonObject.keySet();
			for( String claimName: claimNames ) {
				Object claimValue = jsonObject.get( claimName );
				ret.put( claimName, claimValue );
			}
			return ret;			
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public boolean isValid( String jwt ) {	
		try {
			JWSObject jwsObject = JWSObject.parse( jwt );
			JWSVerifier verifier = new RSASSAVerifier( this.rsaPublicJWK );
			return jwsObject.verify( verifier );
		} 
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}
}
