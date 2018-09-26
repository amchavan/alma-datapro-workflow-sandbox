package alma.obops.draws.messages;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestCouchDB.class, TestExecutor.class, TestMessageBus.class, TestMessageQueue.class })

public class AllTests {

}
