#!/usr/bin/env python3
import sys
import random
import time
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
from dbcon import DbConnection
import dbdrwutils

# Mock-up of AQUA Batch Helper
# TODO

class BatchHelper():
    recipes = [
        "ManualCalibration",
        # Most manual recipes commented out because we don't want
        # too many of them in this mockup
        #"ManualImaging",
        #"ManualSingleDish",
        #"ManualCombination",
        "PipelineCalibration",
        "PipelineImaging",
        "PipelineSingleDish",
        "PipelineCombination",
        "PipelineCalAndImg"
    ]

    def __init__(self):
        self.baseUrl = "http://localhost:5984" # CouchDB
        self.dbconn  = DbConnection(self.baseUrl)

    def start(self):
        self.setRecipes()

    def findReadyForProcessingNoSubstate(self):
    """
        Returns a ReadyForProcessing OUSs with no substate, if any are 
        found; None otherwise
    """
        selector = {
            "selector": {
                "state": "ReadyForProcessing",
                "substate": {"$or": [{ "$eq": None }, { "$eq": "" }]}
            }
        }
        retcode,ouss = self.dbconn.find("status-entities", selector)
        if len(ouss) == 0:
            return None
        if retcode != 200:
            print(ouss)
            return None
        ouss.sort(key=lambda x: x['entityId'])
        return ouss[0]

    def computeRecipe(self, ous):
        "Just pick a recipe at random"
        return random.choice(BatchHelper.recipes)

    def setRecipes(self):
        """
            Runs on a background thread.
            Loop forever:
                Look for ReadyForProcessing OUSs with no Pipeline Recipe
                If you find one:
                    Compute the Pipeline recipe for that OUS
                    Set it
                Sleep some time
        """
        while True:
            ous = self.findReadyForProcessingNoSubstate()
            if ous != None:
                ous["substate"] = self.computeRecipe(ous)
                self.dbconn.save("status-entities", ous["_id"], ous)
                print(">>> OUS:", ous["_id"], "recipe:", ous["substate"])
            
            time.sleep(5)
    

if __name__ == "__main__":
    batch = BatchHelper()
    batch.start()
