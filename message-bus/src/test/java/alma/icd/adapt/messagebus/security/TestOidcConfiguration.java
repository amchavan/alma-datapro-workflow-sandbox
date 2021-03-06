package alma.icd.adapt.messagebus.security;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import alma.icd.adapt.messagebus.security.OidcConfigurationProperties;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = { OidcConfigurationProperties.class })
public class TestOidcConfiguration {

	
	@Autowired
	private OidcConfigurationProperties oidcConfigProps;
	
	@Test
	public void construction() {
		assertNotNull( oidcConfigProps );
		assertNotNull( oidcConfigProps.getServerUrl() );
	}
}
