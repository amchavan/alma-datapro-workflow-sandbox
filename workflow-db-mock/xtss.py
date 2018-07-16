#!/usr/bin/env python3
import sys
sys.path.insert(0, "../shared")
from dbmsgq import Executor
from dbcon import DbConnection
import dbdrwutils
from datetime import datetime

# Mockup of the XTSS, listens on the xtss queue

baseUrl = "http://localhost:5984" # CouchDB
dbcon   = DbConnection( baseUrl )
dbName  = "status-entities"

def __nowISO():
    return datetime.utcnow().strftime( "%Y-%m-%dT%H:%M:%S" )

def findOUSStatus( ousID ):
    "Find an OUSStatus with the given ID, create a new one if none are found"

    retcode,ousStatus = dbcon.findOne( dbName, ousID )
    if retcode == 404:
        # Prepare a new record to write
        ousStatus = {}
        ousStatus['entityId'] = ousID
    return ousStatus

def setField( ousID, fieldName, fieldValue ):
    "Set the value of a field of an OUSStatus, update its timestamp"
    ousStatus = findOUSStatus( ousID )
    ousStatus[fieldName] = fieldValue
    ousStatus['timestamp'] = __nowISO()
    retcode,msg = dbcon.save( dbName, ousID, ousStatus )
    return retcode

def setState( ousID, state ):
    "Set the state of an OUSStatus"
    return setField( ousID, 'state', state )

def setPipelineRecipe( ousID, recipe ):
    "Set the pipeline recipe of an OUSStatus"
    return setField( ousID, 'pipeline-recipe', recipe )

def setExecutive( ousID, executive ):
    "Set the Executive of an OUSStatus"
    return setField( ousID, 'executive', executive )

def xtss( body ):
    """
        Expects the body of the request to be a string including a triplet of words:
            <cmd> <ousID> <value>
        where value depends on the command. For instance:
            set-state uid://A003/X1/X1a ReadyForReview
        or
            set-recipe uid://A003/Xa71/X2c PipelineCalibration

        Returns 201 (created) if all was well.
    """
    print(" [*] request: " + body )
    words = body.split()
    op = words[0]
    if op == "set-state":
        retcode = setState( ousID=words[1], state=words[2] )
        return retcode

    elif op == "set-recipe":
        retcode = setPipelineRecipe( ousID=words[1], recipe=words[2] )
        return retcode

    elif op == "set-exec":
        retcode = setExecutive( ousID=words[1], executive=words[2] )
        return retcode

    else:
        return "Unsupported op: " + op

    return None
	

print(" [x] Awaiting RPC requests to 'xtss'" )
executor = Executor( 'localhost', 'msgq', 'xtss', xtss )
executor.run()
