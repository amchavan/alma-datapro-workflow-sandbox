package alma.obops.draws.messages.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.nimbusds.jose.JOSEException;

public class TestJWTFactory {
	
	private TokenFactory tokenFactory;

	@Before
	public void setUp() throws ParseException, JOSEException {
		this.tokenFactory = new JWTFactory();
	}

	@Test
	public void createStandardToken() {
		String token = tokenFactory.create();
		assertNotNull( token );
		assertTrue( token.length() > 0 );
		
		boolean valid = tokenFactory.isValid( token );
		assertTrue( valid );

		Map<String, Object> claims = tokenFactory.decode( token );
		assertEquals( 3, claims.size() );
		assertEquals( "user", claims.get( "sub" ));
		assertEquals( "admin", claims.get( "role" ));
		Long ttl = (Long) claims.get( "ttl" );
		assertTrue( ttl == 10000L );
	}

	@Test
	public void createValidToken() {
		Map<String, Object> inProps = new HashMap<>();
		inProps.put( "sub",  "amchavan" );
		inProps.put( "role", "admin"    );
		String token = tokenFactory.create( inProps );
		assertNotNull( token );
		assertTrue( token.length() > 0 );
		
		boolean valid = tokenFactory.isValid( token );
		assertTrue( valid );

		Map<String, Object> outProps = tokenFactory.decode( token );
		assertEquals( "amchavan", outProps.get( "sub" ));
		assertEquals( "admin",    outProps.get( "role" ));
	}

	@Test
	public void createInvalidToken() {
		Map<String, Object> inProps = new HashMap<>();
		inProps.put( "sub", "amchavan" );
		String token = tokenFactory.create( inProps );
		assertNotNull( token );
		assertTrue( token.length() > 0 );
		
		// now try to fudge the token
		String[] t = token.split( "\\." );
		String encodedHeader    = t[0];
		String encodedBody      = t[1];
		String encodedSignature = t[2];
		
		String body = new String( Base64.getDecoder().decode( encodedBody ));
		String fudgedBody = body.replace( "amchavan", "pjwoodhouse" );
		String encodedFudgedBody = Base64.getEncoder().encodeToString( fudgedBody.getBytes() );
		String fudgedToken =  encodedHeader + "." + encodedFudgedBody + "." + encodedSignature;

		boolean valid = tokenFactory.isValid( fudgedToken );
		assertFalse( valid );

		try {
			tokenFactory.decode( fudgedToken );
		} 
		catch( Exception e ) {
			// no-op, expected
		}
	}
}
