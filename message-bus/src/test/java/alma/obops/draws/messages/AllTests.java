package alma.obops.draws.messages;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	TestSerialization.class, 
	alma.obops.draws.messages.rabbitmq.AllTests.class, 
	alma.obops.draws.messages.couchdb.AllTests.class,
	alma.obops.draws.messages.security.AllTests.class,
	
	})
public class AllTests {

}
