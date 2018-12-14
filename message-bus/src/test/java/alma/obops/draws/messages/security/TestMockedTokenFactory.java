package alma.obops.draws.messages.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TestMockedTokenFactory {
	
	private TokenFactory tokenFactory;

	@Before
	public void setUp() {
		this.tokenFactory = MockedTokenFactory.getFactory();
	}

	@Test
	public void createStandardToken() {
		String token = tokenFactory.create();
		assertNotNull( token );
		assertTrue( token.length() > 0 );
		
		boolean valid = tokenFactory.isValid( token );
		assertTrue( valid );

		Map<String, Object> claims = tokenFactory.decode( token );
		assertEquals( 3,       claims.size() );
		assertEquals( "user",  claims.get( "sub" ));
		assertEquals( "admin", claims.get( "role" ));
		assertEquals( "10000", claims.get( "ttl" ));
	}

	@Test
	public void createValidToken() {
		Map<String, Object> inProps = new HashMap<>();
		inProps.put( "iss", "amchavan" );
		inProps.put( "role", "admin" );
		String token = tokenFactory.create( inProps );
		assertNotNull( token );
		assertTrue( token.length() > 0 );
		
		boolean valid = tokenFactory.isValid( token );
		assertTrue( valid );

		Map<String, Object> outProps = tokenFactory.decode( token );
		assertTrue( inProps.equals( outProps ));
	}

	@Test
	public void createInvalidToken() {
		Map<String, Object> inProps = new HashMap<>();
		inProps.put( "valid", "false" );
		String token = tokenFactory.create( inProps );
		assertNotNull( token );
		assertTrue( token.length() > 0 );
		
		boolean valid = tokenFactory.isValid( token );
		assertFalse( valid );

		try {
			tokenFactory.decode( token );
		}
		catch( Exception e ) {
			// no-op, expected
		}
	}
}
