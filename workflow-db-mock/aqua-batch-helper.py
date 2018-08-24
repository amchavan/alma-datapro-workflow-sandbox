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

recipes = [
    "ManualCalibration",
    "ManualImaging",
    "ManualSingleDish",
    "ManualCombination",
    "PipelineCalibration",
    "PipelineImaging",
    "PipelineSingleDish",
    "PipelineCombination",
    "PipelineCalAndImg"
]
def findReadyForProcessingNoSubstate():
	"""
		Returns a ReadyForProcessing OUSs with no substate, if any are 
        found; None otherwise
	"""
	selector = {
	   "selector": {
			"state": "ReadyForProcessing",
			"substate": { "$exists": False }
       }
	}
	retcode,ouss = dbconn.find( "status-entities", selector )
	if len( ouss ) == 0:
		return None
	if retcode != 200:
		print( ouss )
		return None

	ouss.sort( key=lambda x: x['entityId'])
	return ouss[0]

def computeRecipe( ous ):
    "Just pick a recipe at random"
    return random.choice( recipes )

def setRecipes():
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
        ous = findReadyForProcessingNoSubstate()
        if ous != None:
            ous["substate"] = computeRecipe( ous )
            print( ">>> saving ", ous )
            dbconn.save( "status-entities", ous["_id"], ous )
        
        time.sleep( 5 )
	

###################################################################
## Main program
###################################################################

baseUrl = "http://localhost:5984" # CouchDB
dbconn  = DbConnection( baseUrl )
setRecipes()




