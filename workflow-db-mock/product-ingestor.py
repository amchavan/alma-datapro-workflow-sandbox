#!/usr/bin/env python3

import base64
import sys
# import random
import argparse
import os
import time
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
from dbcon import DbConnection
from ngascon import NgasConnection
import dbdrwutils

# Mock-up of the Producr Ingestor
# TODO

# def savePlReport( ousUID, timestamp, encodedReport, productsDir, source ):
# 	'''
# 		Saves a pipeline run report to 'Oracle'
# 	'''
# 	plReport = {}
# 	plReport['ousUID'] = ousUID
# 	plReport['timestamp'] = timestamp
# 	plReport['encodedReport'] = encodedReport
# 	plReport['productsDir'] = productsDir
# 	plReport['source'] = source
# 	plReportID = timestamp + "." + ousUID

# 	retcode,msg = dbcon.save( dbName, plReportID, plReport )
# 	if retcode != 201:
# 		raise RuntimeError( "Error saving Pipeline report: %d, %s" % (retcode,msg) )

# def findMostRecentPlReport( ousUID ):
# 	selector = { "selector": { "ousUID": ousUID }}
# 	retcode,reports = dbcon.find( dbName, selector )
# 	if len( reports ) == 0:
# 		return None
# 	if retcode != 200:
# 		print( reports )
# 		return None

# 	# Find the most recent report and return it
# 	reports.sort( key=lambda x: x['timestamp'], reverse=True )
# 	return reports[0]

# def findReadyForReview():
# 	selector = {
# 	   "selector": {
# 	      "state": "ReadyForReview"
# 	   }
# 	}
# 	retcode,ouss = dbcon.find( "status-entities", selector )
# 	if len( ouss ) == 0:
# 		return None
# 	if retcode != 200:
# 		print( ouss )
# 		return None

# 	ouss.sort( key=lambda x: x['entityId'])
# 	return ouss
	
# def processQA2flag( ousUID, flag ):
# 	"Flag should be one of 'F' (fail), 'P' (pass) or 'S' (semi-pass)"
# 	newState = "ReadyForProcessing" if (flag == "F" ) else "Verified"
# 	print( ">>> Setting the state of", ousUID, "to", newState )
# 	# Set the OUS state according to the input flag
# 	dbdrwutils.setState( xtss, ousUID, newState )
def encode( entityID ):
	return entityID.replace( ":", "_" ).replace( "/", "_" )

def setSubstate( ousUID, substate ):
	
	dbcon  = DbConnection( baseUrl )
	dbName  = "status-entities"
	retcode,ousStatus = dbcon.findOne( dbName, ousUID )
	ousStatus['substate'] = substate
	ousStatus['timestamp'] = dbdrwutils.nowISO()
	retcode,retmsg = dbcon.save( dbName, ousUID, ousStatus )
	if retcode != 201:
		raise RuntimeError( "setSubstate: error %d, %s" % (retcode,retmsg) )

def writeMetadata( progID, ousUID, timestamp, dataProduct ):
	# No error checking!
	dbcon  = DbConnection( baseUrl )
	dbName  = "products-metadata"
	metadata = {}
	metadata['progID'] = progID
	metadata['ousUID'] = ousUID
	metadata['timestamp'] = timestamp
	retcode,retmsg = dbcon.save( dbName, dataProduct, metadata )
	if retcode != 201:
		raise RuntimeError( "setSubstate: error %d, %s" % (retcode,retmsg) )

def writeDeliveryStatus( progID, ousUID, timestamp, dataProducts, complete=False ):
	# No error checking!
	dbcon  = DbConnection( baseUrl )
	dbName  = "delivery-status"
	delStatus = {}
	delStatus['progID'] = progID
	delStatus['timestamp'] = timestamp
	delStatus['dataProducts'] = sorted( dataProducts )
	delStatus['complete'] = complete
	delStatus['ousUID'] = ousUID
	retcode,retmsg = dbcon.save( dbName, ousUID, delStatus )
	if retcode != 201:
		raise RuntimeError( "setSubstate: error %d, %s" % (retcode,retmsg) )

def callback( message ):
	"""
	Message is a JSON object:
		ousUID is the UID of the OUS
	 	timestamp is the Pipeline run's timestamp
		productsDir is the name of the products directory for that Pipeline run
	
	For instance
	    { 
	    	"ousUID" : "uid://X1/X1/Xc1", 
	    	"timestamp" : "2018-07-23T09:44:13.604", 
	    	"productsDir" : "2015.1.00657.S_2018_07_23T09_44_13.604"
	    }
	"""

	print( ">>> message:", message )
	request = dbdrwutils.jsonToObj( message )
	ousUID = request.ousUID
	timestamp = request.timestamp
	productsDir = request.productsDir

	setSubstate( ousUID, 'IngestionTriggered' )

	# Locate the data products in the replicated cache dir
	dataProductsDir = os.path.join( args.cache, productsDir, "SOUS", "GOUS", encode(ousUID), "products" )
	dataProdNames = os.listdir( dataProductsDir )
	time.sleep( 5 )	# pretend this actually takes time

	setSubstate( ousUID, 'AnalyzingProducts' )
	time.sleep( 3 )	# pretend this actually takes time

	setSubstate( ousUID, 'IngestingProducts' )
	progID = productsDir.split( '_' )[0]
	ingestedDataProds = []
	for dataProdName in sorted(dataProdNames):
		if dataProdName.startswith( "product" ) and dataProdName.endswith( ".data" ):
			dataProdPathname = os.path.join( dataProductsDir, dataProdName )
			print( ">>> Ingesting:", dataProdPathname )
			ngascon.put( dataProdPathname )
			writeMetadata( progID, ousUID, timestamp, dataProdName )
			ingestedDataProds.append( dataProdName )
			writeDeliveryStatus( progID, ousUID, timestamp, ingestedDataProds )
			time.sleep( 2 )	# pretend this actually takes time

	# Now populate the ASA metadata and delivery status tables
	writeDeliveryStatus( progID, ousUID, timestamp, ingestedDataProds, complete=True )

	setSubstate( ousUID, 'ProductsIngested' )





###################################################################
## Main program
###################################################################
baseUrl = "http://localhost:5984" # CouchDB
ngascon = NgasConnection()

select = "ingest.JAO"
mq     = MqConnection( 'localhost', 'msgq',  select )

parser = argparse.ArgumentParser( description='Product ingestor mock-up' )
parser.add_argument( dest="cache", help="Absolute pathname of the replicating cache dir" )
args=parser.parse_args()

print(' [*] Waiting for messages matching %s' % (select) )
mq.listen( callback )




