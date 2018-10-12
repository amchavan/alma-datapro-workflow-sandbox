package alma.obops.draws.messages.rabbitmq;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestExecutor.class, TestMessageQueue.class, TestPersistence.class })

public class AllTests {

}
