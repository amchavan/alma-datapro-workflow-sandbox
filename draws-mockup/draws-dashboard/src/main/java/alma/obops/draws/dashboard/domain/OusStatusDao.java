package alma.obops.draws.dashboard.domain;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import alma.obops.draws.dashboard.persistence.CouchDbConnection;

@Component
public class OusStatusDao {
	
	public static final String OUS_STATUS_DB_NAME = "status-entities";
	public static final String OUS_STATUS_BY_STATE_SLCTR = "{" + 
			"	\"selector\": {" + 
			"		\"state\": \"%s\"" + 
			"	}" + 
			"}";
	
	@Autowired
	CouchDbConnection cdbConn;

	private Logger logger = LoggerFactory.getLogger( OusStatusDao.class );
	
    public OusStatus[] findByState(String state ) throws IOException {
    	String selector = String.format( OUS_STATUS_BY_STATE_SLCTR, state );
    	OusStatus[] ret = cdbConn.find( OUS_STATUS_DB_NAME, OusStatus[].class, selector );
    	logger.info( "Queryied with selector: " + selector.replaceAll( "\\s", "" ) + ", found: " + ret.length );
		return ret;
    }	
}
