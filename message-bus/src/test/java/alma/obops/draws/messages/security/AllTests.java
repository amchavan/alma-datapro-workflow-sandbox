package alma.obops.draws.messages.security;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestMockedTokenFactory.class, 
	            TestTokenSecurityCouchDB.class, 
	            TestTokenSecurityRabbitMQ.class })

public class AllTests {

}
