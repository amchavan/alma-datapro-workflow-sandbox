package alma.obops.draws.messages.couchdb;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestCouchDB.class, TestCouchDbConfiguration.class, TestExecutor.class, TestMessageBroker.class,
		TestMessageQueue.class })

public class AllTests {

}
