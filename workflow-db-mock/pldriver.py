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
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
import dbdrwutils
from ngascon import NgasConnection
from PLDriver import PLDriver

parser = argparse.ArgumentParser( description='Pipeline Driver mock-up' )
parser.add_argument( dest="progID", help="ID of the project containing the OUS" )
parser.add_argument( dest="ousUID", help="ID of the OUS that should be processed" )
parser.add_argument( dest="recipe", help="Pipeline recipe to run" )
args=parser.parse_args()

print( ">>> PipelineDriver: progID=%s, ousUID=%s, recipe=%s" % (args.progID, args.ousUID, args.recipe ))

#Get from arguments
driver = PLDriver(args.progID, args.ousUID, args.recipe)
driver.run()

sys.exit(0)


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
	print( ">>> PipelineDriver: prodDir:", prodDir )
	return os.path.join( pipelineRunDirectory, prodDir )

def findWeblog( productsDir, ousUID ):
	# DEMO ONLY: the "products" subdirectory should be looked for
	#            here we just take the hardcoded path
	ousUID = dbdrwutils.encode( ousUID )
	productsDir = os.path.join( productsDir, "SOUS", "GOUS", ousUID, "products" )
	for file in os.listdir( productsDir ):
		print( ">>> PipelineDriver: file:", file )
		if (file.startswith( "weblog-" ) and file.endswith( ".zip" )):
			return (os.path.join( productsDir, file ))
	raise RuntimeError( "No weblog found in %s" % productsDir )

def findPipelineReport( productsDir, ousUID ):
	# DEMO ONLY: the "products" subdirectory should be looked for
	#            here we just take the hardcoded path
	ousUID = dbdrwutils.encode( ousUID )
	productsDir = os.path.join( productsDir, "SOUS", "GOUS", ousUID, "products" )
	for file in os.listdir( productsDir ):
		print( ">>> PipelineDriver: file:", file )
		if (file.startswith( "pl-report-" ) and file.endswith( ".xml" )):
			return (os.path.join( productsDir, file ))
	raise RuntimeError( "No pipeline report found in %s" % productsDir )

def runPipeline( progID, ousUID, recipe, pipelineRunDirectory ):
	completed = subprocess.run( [pipelineExecutable, progID, ousUID, recipe], cwd=pipelineRunDirectory)
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
	n = productsDirName.index( '_' )
	timestamp = productsDirName[n+1:]
	timestamp = timestamp.replace( '_', '-', 2 )
	timestamp = timestamp.replace( '_', ':' )
	return timestamp

def wrapPipeline( progID, ousUID, recipe ):
	
	# Run the pipeline on our OUS
	ret = runPipeline( progID, ousUID, recipe, pipelineRunDirectory )
	if ret != 0:
		print( ">>> PipelineDriver: Pipeline returned:", ret )
		if ret != 2:
			raise RuntimeError( ">>> PipelineDriver: Pipeline returned: %d" % ret )
		# Pipeline returned 2 -- processing failed 
		# Push the OUS to ProcessingProblem, and we're done
		dbdrwutils.setState( xtss, ousUID, "ProcessingProblem" )
		return
	
	### Processing was successful!

	# Copy the products directory to the replicating cache directory
	# and signal that to the JAO cache
	productsDir = findProductsDir( progID )
	productsBasedir = os.path.basename( productsDir )
	repCacheDir = os.path.join( replicatedCache, productsBasedir )
	print( ">>> PipelineDriver: Products dir name:", productsDir )
	print( ">>> PipelineDriver: Replicating dir name:", repCacheDir )
	copyAndReplaceDir( productsDir, repCacheDir )

	# message = '{"fileType":"productsdir", "cachedAt":"%s", "name": "%s"}' % (location,productsBasedir)
	message = {}
	message["fileType"] = "productsdir"
	message["cachedAt"] = location
	message["name"]     = productsBasedir
	selector = "cached.JAO"
	mq.send( message, selector )

	# Copy the weblog to the replicating cache directory
	# and signal that to the JAO *and* the local cache  (if
	# they are not one and the same)
	weblog = findWeblog( productsDir, ousUID )
	print( ">>> PipelineDriver: weblog: copying", weblog, "to", replicatedCache )
	shutil.copy( weblog, replicatedCache )

	# message = '{"fileType":"weblog", "cachedAt":"%s", "name": "%s"}' % (location, os.path.basename( weblog ))
	message = {}
	message["fileType"] = "weblog"
	message["cachedAt"] = location
	message["name"]     = os.path.basename( weblog )

	selector = "cached.JAO"
	mq.send( message, selector )
	if replicatedCache != "JAO":
		selector = "cached.%s" % location
		mq.send( message, selector )

	# Send the XML text of the pipeline report to AQUA at JAO 
	# We need to BASE64-encode it because it will be wrapped in a JSON field
	timestamp = getTimestamp( productsBasedir )
	plReportFile = findPipelineReport( productsDir, ousUID )
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
		''' % (ousUID, timestamp, replicatedCache, plReport, productsBasedir)
	message = json.loads( message )		# convert to a Python dict
	selector = "pipeline.report.JAO"
	mq.send( message, selector )
	
	# We are done, set the OUS state to ReadyForReview
	dbdrwutils.setState( xtss, ousUID, "ReadyForReview" )

###################################################################
## Main program
###################################################################

# Make sure we know where we are
location = os.environ.get( 'DRAWS_LOCATION' )
if location == None:
    raise RuntimeError( "DRAWS_LOCATION env variable is not defined" )

# Make sure we know where the local replicated cache directory is
replicatedCache = os.environ.get( 'DRAWS_LOCAL_CACHE' )
if replicatedCache == None:
    raise RuntimeError( "DRAWS_LOCAL_CACHE env variable is not defined" )


pipelineScript = "pipeline.py"
thisDirectory = os.getcwd()		# Assume the pipeline script is in this same directory
pipelineExecutable = thisDirectory + "/" + pipelineScript
pipelineRunDirectory = tempfile.mkdtemp( prefix="drw-" )
workingDirectory = tempfile.mkdtemp( prefix="drw-" )
print( ">>> PipelineDriver: pipelineRunDirectory:", pipelineRunDirectory )
print( ">>> PipelineDriver: workingDirectory:", workingDirectory )

xtss = ExecutorClient( 'localhost', 'msgq', 'xtss' )
mq   = MqConnection( 'localhost', 'msgq' )

wrapPipeline( args.progID, args.ousUID, args.recipe )
