package alma.obops.draws.messages.security;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class MockedTokenFactory implements TokenFactory {

	private static final String HEADER = "header";
	private static final String VALID_SIGNATURE = "valid-signature";
	private static final String INVALID_SIGNATURE = "invalid-signature";

	protected static MockedTokenFactory instance = null;
	
	public static TokenFactory getFactory() {
		if( instance == null ) {
			instance = new MockedTokenFactory(); 
		}
		return instance;
	}
	
	private MockedTokenFactory() {
	}

	@Override
	public String create() {
		Map<String, Object> claims = new HashMap<>();
		claims.put( "sub", "user" );
		claims.put( "role", "admin" );
		claims.put( "ttl", "10000" );
		return create( claims );
	}

	@Override
	public String create( Map<String, Object> properties ) {
		if( properties == null ) {
			throw new IllegalArgumentException( "No properties given" );
		}
		String header = HEADER;
		
		String signature = VALID_SIGNATURE;
		Object valid = properties.get( "valid" );
		if( valid != null && valid.equals( "false" )) {
			signature = INVALID_SIGNATURE;
		}
		StringBuilder bodyBuilder = new StringBuilder();
		for( String propName : properties.keySet() ) {
			Object propValue = properties.get( propName );
			if( bodyBuilder.length() > 0 ) {
				bodyBuilder.append( "," );
			}
			bodyBuilder.append( propName ).append( "=" ).append( propValue );
		}
		
		String body = bodyBuilder.toString();
		String encodedBody = Base64.getEncoder().encodeToString( body.getBytes() );
		return header + "." + encodedBody + "." + signature;
	}

	@Override
	public Map<String,Object> decode( String token ) throws InvalidSignatureException {

		final String encodedBody = isValidInternal( token );
		if( encodedBody == null ) {
			throw new InvalidSignatureException();
		}
		
		String body = new String( Base64.getDecoder().decode( encodedBody ));
		Map<String, Object> properties = new HashMap<>();
		String[] claims = body.split( "," );
		for( int i = 0; i < claims.length; i++ ) {
			String  claim = claims[i];
			String[]  t2 = claim.split( "=" );
			String   key = t2[0];
			String value = t2[1];
			
			properties.put(key, value);
		}
		
		return properties;
	}

	@Override
	public boolean isValid( String token ) {
		return isValidInternal( token ) != null;
	}

	/**
	 * @return <code>null</code> if the input token is invalid, the token body
	 *         otherwise
	 */
	private String isValidInternal( String token ) {
		if( token == null || token.length() == 0 ) {
			return null;
		}
		
		String[] t = token.split( "\\." );
		if( t.length != 3 ) {
			return null;
		}

//		String header    = t[0];	IGNORED
		String body      = t[1];
		String signature = t[2];
		
		if( ! signature.equals( VALID_SIGNATURE )) {
			return null;
		}
		return body;
	}
}
