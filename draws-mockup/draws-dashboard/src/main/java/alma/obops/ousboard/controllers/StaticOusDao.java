package alma.obops.ousboard.controllers;

import alma.obops.ousboard.domain.ObsUnitSet;
import org.json.JSONArray;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@Controller
public class StaticOusDao {

    private static String OUS_LIST_FILE = "./src/main/resources/static/data.json";

    public Set<ObsUnitSet> getObsUnitSetsByState(@RequestParam("state") String state) {
        Set<ObsUnitSet> obsUnitSetSet = new HashSet<>();
        for ( ObsUnitSet obsUnitSet : getAllObsUnitSets() ) {
            if (obsUnitSet.getState().equals(state)) {
                obsUnitSetSet.add(obsUnitSet);
            }
        }
        return obsUnitSetSet;
    }


    public Set<ObsUnitSet> getAllObsUnitSets() {
        Set<ObsUnitSet> obsUnitSetSet = new HashSet<>();

        try {
            String text = new String(Files.readAllBytes(Paths.get(OUS_LIST_FILE)), StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(text);
            for ( int i = 0; i < array.length(); i++  ) {
                    JSONObject ousJson = array.getJSONObject(i);
                    obsUnitSetSet.add(mapToObsUnitSetFromJson(ousJson));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obsUnitSetSet;
    }

    private ObsUnitSet mapToObsUnitSetFromJson(JSONObject ousJson) {
        if ( ousJson == null ) throw new RuntimeException("ousJson is null");
        ObsUnitSet obsUnitSet = new ObsUnitSet();
        obsUnitSet.set_id(ousJson.getString("_id"));
        obsUnitSet.set_rev(ousJson.getString("_rev"));
        obsUnitSet.setEntityId(ousJson.getString("entityId"));
        obsUnitSet.setState(ousJson.getString("state"));
        obsUnitSet.setPipelineRecipe(ousJson.getString("pipeline-recipe"));
        obsUnitSet.setTimestamp(ousJson.getString("timestamp"));
        return  obsUnitSet;
    }


}
