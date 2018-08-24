#!/usr/bin/env python3

import argparse
import sys

sys.path.insert(0, "../shared")
from dbmsgq import MqConnection
from dbcon import DbConnection
import dbdrwutils

###################################################################
## Mocks the Data Tracker setting an OUS ReadyForProcessing
## If needed, creates the OUS entity
## Example:
##      ./launcher.py 2015.1.00657.S uid://X1/X1/Xb2

parser = argparse.ArgumentParser( description='Starter component, creates status entities' )
parser.add_argument( dest="progID", help="ID of the project containing the OUS" )
parser.add_argument( dest="ousUID", help="ID of the OUS that should be processed" )
args = parser.parse_args()
ousUID = args.ousUID
progID = args.progID

dbName  = "status-entities"
baseUrl = "http://localhost:5984" # CouchDB
dbcon = DbConnection( baseUrl )
mq = MqConnection( 'localhost', 'msgq' )

# If we have one already, delete it
retcode,ousStatus = dbcon.findOne( dbName, ousUID )
if retcode == 200:
    dbcon.delete( dbName, ousUID, ousStatus["_rev"] )

# Prepare a new record and write it
ousStatus = {}
ousStatus['entityId'] = ousUID
ousStatus['progID'] = progID
ousStatus['state'] = "ReadyForProcessing"
ousStatus['flags'] = {}
ousStatus['timestamp'] = dbdrwutils.nowISO()
dbcon.save( dbName, ousUID, ousStatus )
