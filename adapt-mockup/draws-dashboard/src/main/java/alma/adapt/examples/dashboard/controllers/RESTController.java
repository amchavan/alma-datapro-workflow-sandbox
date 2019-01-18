package alma.adapt.examples.dashboard.controllers;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alma.adapt.examples.dashboard.domain.OusStatus;
import alma.adapt.examples.dashboard.domain.OusStatusDao;
import alma.icd.adapt.messagebus.configuration.PersistedEnvelopeRepository;
import alma.icd.adapt.messagebus.rabbitmq.PersistedEnvelope;

@CrossOrigin(origins = "*")
@RestController
public class RESTController {


	private Logger logger = LoggerFactory.getLogger( RESTController.class );
	
    @Autowired 
    OusStatusDao ousStatusDao;
    
    @Autowired 
    private PersistedEnvelopeRepository envelopeRepository;

    @RequestMapping("/ous")
    public OusStatus[] getObsUnitSetsByState( @RequestParam("state") String state ) throws IOException {
    	logger.info( ">>> ous: state=" + state );
        final OusStatus[] ret = ousStatusDao.findByState(state);
		return ret;
    }
    
    @RequestMapping("/messages")
	public PersistedEnvelope[] getMessages( /* @RequestParam("state") String state */ ) throws IOException {
    	logger.info( ">>> messages" );
        List<PersistedEnvelope> t = (List<PersistedEnvelope>) envelopeRepository.findAll();
        PersistedEnvelope[] ret = t.toArray( new PersistedEnvelope[0] );
		return ret;
    }
}
