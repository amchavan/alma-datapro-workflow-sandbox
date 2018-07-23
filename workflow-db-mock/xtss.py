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

def findOUSStatus( ousUID ):
    "Find an OUSStatus with the given ID, create a new one if none are found"

    retcode,ousStatus = dbcon.findOne( dbName, ousUID )
    if retcode == 404:
        raise RuntimeError( "OUS not found: %s" % ousUID )
    return ousStatus

def setField( ousUID, fieldName, fieldValue ):
    "Set the value of a field of an OUSStatus, update its timestamp"
    ousStatus = findOUSStatus( ousUID )
    ousStatus[fieldName] = fieldValue
    ousStatus['timestamp'] = __nowISO()
    retcode,msg = dbcon.save( dbName, ousUID, ousStatus )
    return retcode

def setState( ousUID, state ):
    "Set the state of an OUSStatus"
    return setField( ousUID, 'state', state )

def setSubstate( ousUID, substate ):
    "Set the substate of an OUSStatus"
    return setField( ousUID, 'substate', substate )

# def setPipelineRecipe( ousUID, recipe ):
#     "Set the pipeline recipe of an OUSStatus"
#     return setField( ousUID, 'pipeline-recipe', recipe )

def setExecutive( ousUID, executive ):
    "Set the Executive of an OUSStatus"
    return setField( ousUID, 'executive', executive )

def xtss( body ):
    """
        Expects the body of the request to be a JSON document including fields
        "operation", "ousUID" and "value":
            {"operation":"...", "ousUID":"...", "value":"..."}
        where value depends on the command. For instance:
            { 
                "operation":"set-state", 
                "ousUID":"uid://A003/X1/X1a", 
                "value":"ReadyForReview"
            }

        Returns 201 (created) if all was well.
    """
    print(" [*] request: " + body )
    request = dbdrwutils.jsonToObj( body )
    operation = request.operation
    ousUID = request.ousUID
    value = request.value

    if operation == "set-state":
        retcode = setState( ousUID=ousUID, state=value )
        return retcode

    elif operation == "set-substate":
        retcode = setSubstate( ousUID=ousUID, substate=value )
        return retcode

    elif operation == "set-exec":
        retcode = setExecutive( ousUID=ousUID, executive=value )
        return retcode

    else:
        return "Unsupported operation: " + operation

    return None
	

executor = Executor( 'localhost', 'msgq', 'xtss', xtss )
print(" [x] Awaiting RPC requests to 'xtss'" )
executor.run()
