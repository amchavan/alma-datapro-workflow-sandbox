#!/usr/bin/env python3

import os
import argparse
import subprocess
import datetime
import tempfile
import shutil
import sys
import json
import base64
import time
import threading
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
import dbdrwutils
from ngascon import NgasConnection

# Mock-up of the Pipeline Driver (successor of DARED)
# It pretends to be running at one of the executives

def isProductsDirectory( f, progID ):
	return (not os.path.isfile( os.path.join( pipelineRunDirectory, f)) and f.startswith( progID ))

def findProductsDir( progID ):
	"Get the most recent product directory"
	allFiles = os.listdir( pipelineRunDirectory )
	prodDirs = [f for f in allFiles if isProductsDirectory( f, progID )]
	prodDir = sorted( prodDirs )[-1:]
	prodDir = prodDir[0]
	print( ">>> prodDir:", prodDir )
	return os.path.join( pipelineRunDirectory, prodDir )

def findWeblog( productsDir ):
	# DEMO ONLY: the "products" subdirectory should be looked for
	#            here we just take the hardcoded path
	productsDir = productsDir + "/SOUS/GOUS/MOUS/products"
	for file in os.listdir( productsDir ):
		print( ">>> file:", file )
		if (file.startswith( "weblog-" ) and file.endswith( ".zip" )):
			return (os.path.join( productsDir, file ))
	raise RuntimeError( "No weblog found in %s" % productsDir )

def findPipelineReport( productsDir ):
	# DEMO ONLY: the "products" subdirectory should be looked for
	#            here we just take the hardcoded path
	productsDir = productsDir + "/SOUS/GOUS/MOUS/products"
	for file in os.listdir( productsDir ):
		print( ">>> file:", file )
		if (file.startswith( "pl-report-" ) and file.endswith( ".xml" )):
			return (os.path.join( productsDir, file ))
	raise RuntimeError( "No pipeline report found in %s" % productsDir )

def runPipeline( progID, ousUID, executive, pipelineRunDirectory ):
	completed = subprocess.run( [pipelineExecutable, progID, ousUID, executive], cwd=pipelineRunDirectory)
	return completed.returncode

def copyAndReplaceDir( from_path, to_path ):
    if os.path.exists( to_path ):
        shutil.rmtree( to_path )
    shutil.copytree( from_path, to_path )

def getTimestamp( productsDirName ):
	'''
		If productsDirName is something like '2015.1.00657.S_2018_07_19T08_50_10.228' 
		will return 2018-07-19T08:50:10.228
	'''
	# print( ">>> getTimestamp: got:", productsDirName )
	n = productsDirName.index( '_' )
	timestamp = productsDirName[n+1:]
	timestamp = timestamp.replace( '_', '-', 2 )
	timestamp = timestamp.replace( '_', ':' )
	return timestamp

def callback( message ):
	""" 
	Executing Pipeline runs, resources permitting
	"""
	dbdrwutils.bgRun( processRunPipelineMessage, (message,))
	time.sleep( 0.5 )	# Give some time to the thread to start up
						# (just in case)

def processRunPipelineMessage( message ):
	""" 
	Body of the message is something like 
	      { "progID":"...", "ousUID":"...", "recipe":"..." }
	where:
	 	progID is the ObservingProgram ID 
	 	ousUID is the UID of the OUS
	    recipe is the Pipeline processing recipe (currently ignored)
	
	For instance
	    {
			"progID":"2015.1.00657.S", 
			"ousUID":"uid://X1/X1/Xaf", 
			"recipe":"PipelineCalibration"
		}
	"""	
	print( ">>> message:", message )
	request = dbdrwutils.jsonToObj( message )
	progID = request.progID
	ousUID = request.ousUID
	recipe = request.recipe		# Ignored
	
	# Set the OUS state to Processing
	dbdrwutils.setState( xtss, ousUID, "Processing" )

	# Run the pipeline on our OUS
	ret = runPipeline( progID, ousUID, args.exec, pipelineRunDirectory )
	if ret != 0:
		print( ">>> Pipeline returned:", ret )
		if ret != 2:
			raise RuntimeError( "Pipeline returned: %d" % ret )
		# Pipeline returned 2 -- processing failed 
		# Push the OUS to ProcessingProblem, and we're done
		dbdrwutils.setState( xtss, ousUID, "ProcessingProblem" )
		return
	
	### Processing was successful!

	# Copy the products directory to the replicating cache directory
	# and signal that to the JAO cache
	productsDir = findProductsDir( progID )
	productsBasedir = os.path.basename( productsDir )
	repCacheDir = os.path.join( args.cache, productsBasedir )
	print( ">>> Products dir name:", productsDir )
	print( ">>> Replicating dir name:", repCacheDir )
	copyAndReplaceDir( productsDir, repCacheDir )

	message = '{"fileType":"productsdir", "cachedAt":"%s", "name": "%s"}' % (args.exec,productsBasedir)
	selector = "cached.JAO"
	mq.send( message, selector )

	# Copy the weblog to the replicating cache directory
	# and signal that to the JAO *and* the local cache  (if
	# they are not one and the same)
	weblog = findWeblog( productsDir )
	print( ">>> weblog: copying", weblog, "to", args.cache )
	shutil.copy( weblog, args.cache )

	message = '{"fileType":"weblog", "cachedAt":"%s", "name": "%s"}' % (args.exec, os.path.basename( weblog ))
	selector = "cached.JAO"
	mq.send( message, selector )
	if args.exec != "JAO":
		selector = "cached.%s" % args.exec
		mq.send( message, selector )

	# Send the XML text of the pipeline report to AQUA at JAO 
	# We need to BASE64-encode it because it will be wrapped in a JSON field
	timestamp = getTimestamp( productsBasedir )
	plReportFile = findPipelineReport( productsDir )
	plReport = dbdrwutils.readTextFileIntoString( plReportFile )
	plReport = dbdrwutils.b64encode( plReport )
	message = '''
		{ 	
			"ousUID" : "%s", 
			"timestamp" : "%s",
			"source" : "%s", 
			"report" : "%s", 
			"productsDir": "%s"
		}
		''' % (ousUID, timestamp, args.exec, plReport, productsBasedir)
	message = message.replace( '\n','')
	selector = "pipeline.report.JAO"
	mq.send( message, selector )
	
	# We are done, set the OUS state to ReadyForReview
	dbdrwutils.setState( xtss, ousUID, "ReadyForReview" )


def availableExecutors():
	"""
		Return 	True if we have at least one available executor, 
				False if all available executors were taken up and we must wait 
	"""
	print( ">>>>>>> threads: active: ", threading.active_count(), "max:", maxThreads )
	return threading.active_count() < maxThreads

###################################################################
## Main program
###################################################################

pipelineScript = "pipeline.py"
thisDirectory = os.getcwd()		# Assume the pipeline script is in this same directory
pipelineExecutable = thisDirectory + "/" + pipelineScript
pipelineRunDirectory = tempfile.mkdtemp( prefix="drw-" )
workingDirectory = tempfile.mkdtemp( prefix="drw-" )
print( "pipelineRunDirectory:", pipelineRunDirectory )
print( "workingDirectory:", workingDirectory )

parser = argparse.ArgumentParser( description='Pipeline Driver mock-up' )
parser.add_argument( dest="exec",  help="Where this driver is running: one of 'EA', 'EU', 'JAO' or 'NA'" )
parser.add_argument( dest="cache", help="Absolute pathname of the replicating cache dir" )
parser.add_argument( "--concurrent-runs", "-r", dest="maxRuns", help="Max number of concurrent Pipeline runs, default=1", default=1 )

args=parser.parse_args()

listen_to = ("pipeline.process.%s" % args.exec)
mq = MqConnection( 'localhost', 'msgq',  listen_to )
xtss = ExecutorClient( 'localhost', 'msgq', 'xtss' )
maxThreads = int(args.maxRuns) + 1 	# One thread per Pipeline run, plus the main thread

print(' [*] Waiting for messages matching %s' % (listen_to) )
# If there is an available executor, listen to messages
mq.listen( callback, condition=availableExecutors )	