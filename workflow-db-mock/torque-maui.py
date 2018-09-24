#!/usr/bin/env python3

import sys
# import random
import argparse
import os
import time
import threading
import subprocess
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection
import dbdrwutils

# Mock-up of Torque/Maui
# The job submission API is simulated by listening to a message sent
# to pipeline.processing.<exec>, where <exec> is the value of the 
# DRAWS_LOCATION environment variable

def runPipelineDriver( progID, ousUID, recipe ):
	completed = subprocess.run( [pipelineDriverExecutable, progID, ousUID, recipe] )
	return completed.returncode

def processRunPipelineMessage( message ):
	runPipelineDriver( message["progID"], message["ousUID"], message["recipe"] )

def callback( message ):
	"""
	Message is a JSON object:
		progID  ID of the project containing the OUS
        ousUID  ID of the OUS that should be processed
        recipe  Pipeline recipe to run
	
	For instance
	    { 
	    	"ousUID" : "uid://X1/X1/Xc1", 
	    	"recipe" : "PipelineSingleDish", 
	    	"progID" : "2015.1.00657.S"
	    }
	"""
	print( ">>> Torque: message:", message )
	dbdrwutils.bgRun( processRunPipelineMessage, (message,))
	time.sleep( 0.5 )	# Give some time to the thread to start up
						# (just in case)

def availableExecutors():
	"""
		Return 	True if we have at least one available executor, 
				False if all available executors were taken up and we must wait 
	"""
	# print( ">>>>>>> threads: active: ", threading.active_count(), "max:", maxThreads )
	return threading.active_count() < maxThreads

###################################################################
## Main program
###################################################################

baseUrl = "http://localhost:5984" # CouchDB

# Make sure we know where we are
location = os.environ.get( 'DRAWS_LOCATION' )
if location == None:
    raise RuntimeError( "DRAWS_LOCATION env variable is not defined" )

parser = argparse.ArgumentParser( description='Pipeline Driver mock-up' )
parser.add_argument( "--max-concurrent-executions", "-x", \
					 dest="maxExecutions", \
					 help="Max number of concurrent Pipeline executions, default=1", \
					 default=1 )
args=parser.parse_args()

pipelineDriverScript = "pipeline-driver.py"
thisDirectory = os.getcwd()		# Assume the pipeline driver script is in this same directory
pipelineDriverExecutable = thisDirectory + "/" + pipelineDriverScript


maxThreads = int(args.maxExecutions) + 1 	# One thread per Pipeline run, plus the main thread
select  = "pipeline.process." + location
mq      = MqConnection( 'localhost', 'msgq',  select )
print(' [*] Waiting for messages matching %s' % (select) )
mq.listen( callback, condition=availableExecutors )




