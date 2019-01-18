package alma.icd.adapt.messagebus.couchdb;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import alma.icd.adapt.messagebus.configuration.CouchDbConfiguration;
import alma.icd.adapt.messagebus.configuration.CouchDbConfigurationProperties;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CouchDbConfiguration.class, CouchDbConfigurationProperties.class})
public class TestCouchDbConfiguration {

	
	@Autowired
	private CouchDbConfigurationProperties couchDbConfigProps;
	

	@Test
	public void construction() {
		assertNotNull( couchDbConfigProps );
		assertNotNull( couchDbConfigProps.getConnection() );
	}
}
