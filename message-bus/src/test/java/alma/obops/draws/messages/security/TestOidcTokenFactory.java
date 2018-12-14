package alma.obops.draws.messages.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import net.minidev.json.JSONArray;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { OidcConfigurationProperties.class, OidcTokenFactory.class })
public class TestOidcTokenFactory {
	
//	@Autowired
//	private OidcConfigurationProperties oidcConfigProps;
	
	@Autowired
	private OidcTokenFactory tokenFactory;

	@Test
	public void createStandardToken() {
		
		String token = tokenFactory.create();
		assertNotNull( token );
		assertTrue( token.length() > 0 );
		
		boolean valid = tokenFactory.isValid( token );
		assertTrue( valid );

		Map<String, Object> claims = tokenFactory.decode( token );
		assertEquals( "obops", claims.get( "sub" ));
		JSONArray roles = (JSONArray) claims.get( "roles" );
		assertTrue( roles.contains( "MASTER/USER" ) );
		Date exp = (Date) claims.get( "exp" );
		assertTrue( exp.getTime() > new Date().getTime() );
	}
}
