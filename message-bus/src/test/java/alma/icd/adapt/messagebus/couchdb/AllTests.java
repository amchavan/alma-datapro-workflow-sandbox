package alma.icd.adapt.messagebus.couchdb;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestCouchDB.class, TestCouchDbConfiguration.class, TestExecutor.class, TestMessageBroker.class,
		TestPublisherSubscriber.class })

public class AllTests {

}
