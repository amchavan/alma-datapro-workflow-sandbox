#!/usr/bin/env python3

import argparse
import sys

sys.path.insert(0, "../shared")
from dbmsgq import MqConnection
from dbcon import DbConnection
import dbdrwutils

###################################################################
## Main program
###################################################################

parser = argparse.ArgumentParser( description='Temporary component, creates status entities and launches a Pipeline run' )
parser.add_argument( dest="progID", help="ID of the project containing the OUS" )
parser.add_argument( dest="ousUID",  help="ID of the OUS that should be processed" )
parser.add_argument( dest="recipe", help="Piepeline recipe" )
parser.add_argument( dest="exec",   help="Executive where this pipeline is running" )
args = parser.parse_args()
ousUID = args.ousUID
progID = args.progID
recipe = args.recipe
executive = args.exec

dbName  = "status-entities"
baseUrl = "http://localhost:5984" # CouchDB
dbcon = DbConnection( baseUrl )
mq = MqConnection( 'localhost', 'msgq' )

retcode,ousStatus = dbcon.findOne( dbName, ousUID )
if retcode == 404:
    # Prepare a new record and write it
    ousStatus = {}
    ousStatus['entityId'] = ousUID
    dbcon.save( dbName, ousUID, ousStatus )

# Set the proper state now
retcode,ousStatus = dbcon.findOne( dbName, ousUID )
ousStatus['state'] = "ReadyForProcessing"
ousStatus['timestamp'] = dbdrwutils.nowISO()
dbcon.save( dbName, ousUID, ousStatus )

dbdrwutils.broadcastPipelineProcess( mq, progID, ousUID, recipe, executive )
