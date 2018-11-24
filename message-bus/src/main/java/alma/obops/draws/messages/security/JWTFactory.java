package alma.obops.draws.messages.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

public class JWTFactory implements TokenFactory {

	// The secret key. This should be in a property file, NOT under source
    // control and not hard coded in real life. It's here for simplicity.
    private static final String SECRET_KEY = 
    		"oeRaYY7Wo24sDqKSX3IM9ASGmdGPmkTd9jo1QTy4b7P9Ze5_9hKolVX8xNrQDcNRfVEdTZNOuOyqEGhXEbdJI-ZQ19k_o9MI0y3eZN2lp9jow55FfXMiINEdt1XR85VipRLSOkT6kSpzs2x-jbLDiz9iFVzkd81YKxMgPA7VfZeQUm4n-mOmnWMaVX30zGFU4L3oPBctYKkl4dYfqYWqRNfrgPJVi5DGFjywgxx0ASEiJHtV72paI3fDR2XwlSkyhhmY-ICjCRmsJN4fX1pdoL8a18-aQrvyu4j0Os6dVPYIoPvvY0SAZtWYKHfM15g7A3HD4cVREf9cUsprCRK93w";
	private static final String TIME_TO_LIVE_CLAIM = "ttl";

	protected static JWTFactory instance = null;
	
	public static TokenFactory getFactory() {
		if( instance == null ) {
			instance = new JWTFactory(); 
		}
		return instance;
	}
	
	private JWTFactory() {
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
		
	    //The JWT signature algorithm we will be using to sign the token
	    SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

	    long nowMillis = System.currentTimeMillis();
	    Date now = new Date( nowMillis );

	    //We will sign our JWT with our ApiKey secret
	    byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary( SECRET_KEY );
	    Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

	    // See if caller specified a TTL, convert that to an expiration time
	    Date exp = null;
	    Object ttl = claims.get( TIME_TO_LIVE_CLAIM );
	    if( ttl != null && Number.class.isAssignableFrom( ttl.getClass() )) {
	    	
	    	Number ttln = (Number) ttl;
	    	long ttlMillis = ttln.longValue();
	    	if (ttlMillis > 0) {
		        long expMillis = nowMillis + ttlMillis;
		        exp = new Date( expMillis );
		    }
	    	claims.remove( TIME_TO_LIVE_CLAIM );
	    }
	    JwtBuilder builder = Jwts.builder()
	    		.setClaims( claims )
	            .setIssuedAt( now )
	    		.signWith( signatureAlgorithm, signingKey );

	    if( exp != null ) {
	    	builder.setExpiration(exp);
	    }	      
	  
	    //Builds the JWT and serializes it to a compact, URL-safe string
	    return builder.compact();
	}

	@Override
	public Map<String,Object> decode( String jwt ) throws InvalidSignatureException {
		
		//This line will throw an exception if it is not a signed JWS (as expected)
	    Claims claims;
		try {
			claims = Jwts.parser()
			        .setSigningKey(DatatypeConverter.parseBase64Binary( SECRET_KEY ))
			        .parseClaimsJws(jwt).getBody();
		} 
		catch( SignatureException e ) {
			throw new InvalidSignatureException( e );
		}
	   
	    Map<String,Object> ret = new HashMap<>();
	    for (Entry<String, Object> claim : claims.entrySet() ) {
			String key = claim.getKey();
			Object value = claim.getValue();
			ret.put( key, value );
		}
	    return ret;
	}

	@Override
	public boolean isValid( String token ) {
		try {
			decode( token );
		}
		catch( InvalidSignatureException e ) {
			return false;
		}
		return true;
	}
}
