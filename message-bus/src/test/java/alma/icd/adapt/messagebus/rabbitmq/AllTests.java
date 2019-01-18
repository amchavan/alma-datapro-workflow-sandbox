package alma.icd.adapt.messagebus.rabbitmq;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestExecutor.class, TestPersistence.class, TestPublisherSubscriber.class })

public class AllTests {

}
