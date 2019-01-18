package alma.icd.adapt.messagebus.security;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
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
	private static SimpleDateFormat isoDateFormat;
	
    public static final String TIME_TO_LIVE_CLAIM = "ttl";
	public static final String EXPIRES_CLAIM = "exp";
	public static final String ROLES_CLAIM = "roles";
	public static final long TIME_TO_LIVE = 10000L;
    public static final String ISOTIMEDATESTRING = "yyyy-MM-dd'T'HH:mm:ss";
	
    private RSAKey rsaJWK;
	private RSAKey rsaPublicJWK;
	private JWSSigner signer;
	
	public JWTFactory(){
		try {
			this.rsaJWK = RSAKey.parse( JWKS );
			this.rsaPublicJWK = rsaJWK.toPublicJWK();
			this.signer = new RSASSASigner(rsaJWK);
		} 
		catch( Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public String create() {
		
		Map<String, Object> claims = new HashMap<>();
		claims.put( "sub", "user" );
		claims.put( "roles", "[\"MASTER/USER\",\"OBOPS/AOD\"]" );
		return create( claims );
	}

	@Override
	public String create( Map<String, Object> claims ) {

		claims.put( TIME_TO_LIVE_CLAIM, TIME_TO_LIVE );
		
		Date now = new Date();
		Date expiresDate = new Date( now.getTime() + TIME_TO_LIVE );
		String expires = getIsoDateFormat().format( expiresDate );
		claims.put( EXPIRES_CLAIM, expires );
		
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
		if( ! isValid( jwt )) {
			throw new RuntimeException( "Invalid JWT: " + jwt );
		}
		
		try {
			Map<String,Object> ret = new HashMap<>();
			JWSObject jwsObject = JWSObject.parse( jwt );
			final JSONObject jsonObject = jwsObject.getPayload().toJSONObject();
			Set<String> claimNames = jsonObject.keySet();
			for( String claimName: claimNames ) {
				Object claimValue = jsonObject.get( claimName );
				
				// Special case: expiry date
				if( claimName.equals( EXPIRES_CLAIM )) {
					claimValue = getIsoDateFormat().parse( claimValue.toString() );
				}
				ret.put( claimName, claimValue );
				
				// Special case: roles
				if( claimName.equals( ROLES_CLAIM )) {
					ObjectMapper mapper = new ObjectMapper();
					String[] t = mapper.readValue( claimValue.toString(), String[].class );
					claimValue = Arrays.asList( t );
				}
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
	
	public static SimpleDateFormat getIsoDateFormat() {
		if( isoDateFormat == null ) {
			TimeZone timezone = TimeZone.getTimeZone( "Etc/GMT" );
			isoDateFormat = new SimpleDateFormat( ISOTIMEDATESTRING );
			isoDateFormat.setTimeZone(timezone);
		}
		return isoDateFormat;
	}
}
