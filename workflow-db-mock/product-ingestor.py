#!/usr/bin/env python3

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

# Mock-up of the Product Ingestor
# TODO

def encode( entityID ):
	return entityID.replace( ":", "_" ).replace( "/", "_" )

def setSubstate( ousUID, substate ):
	dbdrwutils.setSubstate( xtss, ousUID, substate )

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
	ousUID = message["ousUID"]
	timestamp = message["timestamp"]
	productsDir = message["productsDir"]

	setSubstate( ousUID, 'IngestionTriggered' )

	# Locate the data products in the replicated cache dir
	dataProductsDir = os.path.join( localCache, productsDir, "SOUS", "GOUS", encode(ousUID), "products" )
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

xtss   = ExecutorClient( 'localhost', 'msgq', 'xtss' )

select = "ingest.JAO"
mq     = MqConnection( 'localhost', 'msgq',  select )

# Make sure we know where the local replicated cache directory is
localCache = os.environ.get( 'DRAWS_LOCAL_CACHE' )
if localCache == None:
    raise RuntimeError( "DRAWS_LOCAL_CACHE env variable is not defined" )

print(' [*] Waiting for messages matching', select )
mq.listen( callback )




