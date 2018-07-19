#!/usr/bin/env python3

import base64
import sys
from time import sleep
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
from dbcon import DbConnection
import dbdrwutils

# Mock-up of AQUA
# TODO

def savePlReport( ousUID, timestamp, encodedReport, productsDir, source ):
	'''
		Saves a pipeline run report to 'Oracle'
	'''
	plReport = {}
	plReport['ousUID'] = ousUID
	plReport['timestamp'] = timestamp
	plReport['encodedReport'] = encodedReport
	plReport['productsDir'] = productsDir
	plReport['source'] = source
	plReportID = timestamp + "." + ousUID

	retcode,msg = dbconn.save( dbName, plReportID, plReport )
	if retcode != 201:
		raise RuntimeError( "Error saving Pipeline report: %d, %s" % (retcode,msg) )

def findMostRecentPlReport( ousUID ):
	selector = { "selector": { "ousUID": ousUID }}
	retcode,reports = dbconn.find( dbName, selector )
	if len( reports ) == 0:
		return None
	if retcode != 200:
		print( reports )
		return None

	# Find the most recent report and return it
	reports.sort( key=lambda x: x['timestamp'], reverse=True )
	return reports[0]

def findReadyForReview():
	selector = {
	   "selector": {
	      "state": "ReadyForReview"
	   }
	}
	retcode,ouss = dbconn.find( "status-entities", selector )
	if len( ouss ) == 0:
		return None
	if retcode != 200:
		print( ouss )
		return None

	ouss.sort( key=lambda x: x['entityId'])
	return ouss
	
def processQA2flag( ousUID, flag ):
	"Flag should be one of 'F' (fail), 'P' (pass) or 'S' (semi-pass)"
	newState = "ReadyForProcessing" if (flag == "F" ) else "Verified"
	print( ">>> Setting the state of", ousUID, "to", newState )
	# Set the OUS state according to the input flag
	dbdrwutils.setState( xtss, ousUID, newState )


def callback( message ):
	"""
	Message is a JSON object:
		ousUID is the UID of the OUS
	 	source is the executive where the Pipeline was running 
	 	report is the report's XML text, BASE64-encoded
	 	timestamp is the Pipeline run's timestamp
		productsDir is the name of the products directory for that Pipeline run
	
	For instance
	    {
	    	"ousUID" : "uid://X1/X1/Xaf", 
	    	"source" : "EU", 
	      	"report" : "Cjw/eG1sIHZlcnNpb2..."
	     	"timestamp" : "2018-07-19T08:50:10.228", 
			"productsDir": "2015.1.00657.S_2018_07_19T08_50_10.228"
		}
	"""

	print( ">>> message:", message[:80], "..." )
	request = dbdrwutils.jsonToObj( message )
	ousUID = request.ousUID
	source = request.source	
	encodedReport = request.report
	timestamp = request.timestamp
	productsDir = request.productsDir
	# report = dbdrwutils.b64decode( encodedReport )
	# print( ">>> report:", report )
	
	# Save the report to Oracle
	savePlReport( ousUID, timestamp, encodedReport, productsDir, source )



###################################################################
## Main program
###################################################################

baseUrl = "http://localhost:5984" # CouchDB
dbconn  = DbConnection( baseUrl )
dbName  = "pipeline-reports"

xtss   = ExecutorClient( 'localhost', 'msgq', 'xtss' )
select = "pipeline.report.JAO"
mq     = MqConnection( 'localhost', 'msgq',  select )

# Launch the listener in the backgrounf
print(' [*] Waiting for messages matching %s' % (select) )
dbdrwutils.bgRun( mq.listen, (callback,) )
# mq.listen( callback )

# Loop forever:
#   Show Pipeline runs awaiting review
#	Ask for an OUS UID
#   Lookup the most recent PL execution for that
#	Print it out
#   Ask for Fail, Pass, or SemiPass
#	Set the OUS state accordingly
while True:
	print()
	print()
	print('------------------------------------------')
	print()

	print( "OUSs ready to be reviewed")
	ouss = findReadyForReview()
	if (ouss == None or len( ouss ) == 0):
		print( "(none)" )
	else:
		for ous in ouss:
			print( ous['entityId'] )

	print()
	ousUID = input( 'Please enter an OUS UID: ' )
	plReport = findMostRecentPlReport( ousUID )
	if plReport == None:
		print( "No Pipeline executions for OUS", ousUID )
		continue

	# We are reviewing this OUS, set its state accordingly
	dbdrwutils.setState( xtss, ousUID, "Reviewing" )

	timestamp = plReport['timestamp']
	report = dbdrwutils.b64decode( plReport['encodedReport'] )
	productsDir = plReport['productsDir']
	source = plReport['source']

	# This is the program's command-line UI
	print( "Pipeline report for UID %s, processed %s" % (ousUID,timestamp))
	print( report )
	while True:
		reply = input( "Enter [F]ail, [P]ass, [S]emipass, [C]ancel: " )
		reply = reply[0:1].upper()
		if ((reply=='F') or (reply=='P') or (reply=='S') or (reply=='C')):
			break
	if reply == 'C':
		continue

	# Set the OUS state according to the QA2 flag
	processQA2flag( ousUID, reply )

	if reply == 'F':
		continue

	# Tell the Product Ingestor that it should ingest those Pipeline products
	selector = "ingest.JAO"
	message = '{"ousUID" : "%s", "timestamp" : "%s", "source" : "%s", "productsDir" : "%s"}' % \
		(ousUID, timestamp, source, productsDir)
	mq.send( message, selector )

	# Now we can set the state of the OUS to DeliveryInProgress
	dbdrwutils.setState( xtss, ousUID, "DeliveryInProgress" )



