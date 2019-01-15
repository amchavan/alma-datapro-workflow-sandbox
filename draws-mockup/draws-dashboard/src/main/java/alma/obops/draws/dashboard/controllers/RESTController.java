package alma.obops.draws.dashboard.controllers;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alma.obops.draws.dashboard.domain.OusStatus;
import alma.obops.draws.dashboard.domain.OusStatusDao;
import alma.obops.draws.dashboard.persistence.envelope.PersistedEnvelope;
import alma.obops.draws.dashboard.persistence.envelope.PersistedEnvelopeRepository;

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
        return ousStatusDao.findByState(state);
    }
    
    @RequestMapping("/messages")
	public List<PersistedEnvelope> getMessages( /* @RequestParam("state") String state */ ) throws IOException {
    	logger.info( ">>> messages" );
        List<PersistedEnvelope> ret = envelopeRepository.findAllSorted();
		return ret;
    }
}
