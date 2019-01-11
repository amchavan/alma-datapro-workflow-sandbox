package alma.obops.ousboard.controllers;

import alma.obops.ousboard.domain.ObsUnitSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
public class RESTController {

    @Autowired StaticOusDao staticOusDao;


    @RequestMapping("/ous")
    public Set<ObsUnitSet> getObsUnitSetsByState(@RequestParam("state") String state) {
        return staticOusDao.getObsUnitSetsByState(state);
    }


}
