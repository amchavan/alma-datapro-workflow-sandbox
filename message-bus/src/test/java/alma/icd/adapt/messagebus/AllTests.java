package alma.icd.adapt.messagebus;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	TestSerialization.class, 
	alma.icd.adapt.messagebus.rabbitmq.AllTests.class, 
	alma.icd.adapt.messagebus.couchdb.AllTests.class,
	alma.icd.adapt.messagebus.security.AllTests.class,
	
	})
public class AllTests {

}
