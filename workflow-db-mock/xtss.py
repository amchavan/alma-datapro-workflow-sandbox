#!/usr/bin/env python3
import sys
import json
import datetime
from collections import namedtuple
sys.path.insert(0, "../shared")
from dbmsgq import Executor
from dbcon import DbConnection
import dbdrwutils

# Mockup of the XTSS, listens on the xtss queue

baseUrl = "http://localhost:5984" # CouchDB
dbcon   = DbConnection( baseUrl )
dbName  = "status-entities"

def __nowISO():
    return datetime.datetime.utcnow().isoformat()[:-3]

def findOUSStatus( ousID ):
    "Find an OUSStatus with the given ID, create a new one if none are found"

    retcode,ousStatus = dbcon.findOne( dbName, ousID )
    if retcode == 404:
        raise RuntimeError( "OUS not found: %s" % ousID )
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
        Expects the body of the request to be a JSON document including fields
        "operation", "ousID" and "value":
            {"operation":"...", "ousID":"...", "value":"..."}
        where value depends on the command. For instance:
            { 
                "operation":"set-state", 
                "ousID":"uid://A003/X1/X1a", 
                "value":"ReadyForReview"
            }

        Returns 201 (created) if all was well.
    """
    print(" [*] request: " + body )
    request = dbdrwutils.jsonToObj( body )
    operation = request.operation
    ousID = request.ousID
    value = request.value

    if operation == "set-state":
        retcode = setState( ousID=ousID, state=value )
        return retcode

    elif operation == "set-recipe":
        retcode = setPipelineRecipe( ousID=ousID, recipe=value )
        return retcode

    elif operation == "set-exec":
        retcode = setExecutive( ousID=ousID, executive=value )
        return retcode

    else:
        return "Unsupported operation: " + operation

    return None
	

executor = Executor( 'localhost', 'msgq', 'xtss', xtss )
print(" [x] Awaiting RPC requests to 'xtss'" )
executor.run()
