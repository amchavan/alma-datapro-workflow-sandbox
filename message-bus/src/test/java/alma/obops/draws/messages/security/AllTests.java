package alma.obops.draws.messages.security;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestJWTFactory.class, TestOidcConfiguration.class, TestOidcTokenFactory.class,
		TestTokenSecurityCouchDB.class, TestTokenSecurityRabbitMQ.class })

public class AllTests {

}
