package alma.obops.draws.dashboard.controllers;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alma.obops.draws.dashboard.domain.OusStatus;
import alma.obops.draws.dashboard.domain.OusStatusDao;

@CrossOrigin(origins = "*")
@RestController
public class RESTController {

    @Autowired OusStatusDao ousStatusDao;

    @RequestMapping("/ous")
    public OusStatus[] getObsUnitSetsByState( @RequestParam("state") String state ) throws IOException {
        return ousStatusDao.findByState(state);
    }
}
