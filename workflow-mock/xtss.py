#!/usr/bin/env python3
import sys
sys.path.insert(0, "../shared")
from msgq import Executor, Filter
from dbcon import DbConnection
import drwutils

# Mockup of the XTSS, listens on the xtss queue

baseUrl = "http://127.0.0.1:5984" # CouchDB
dbcon   = DbConnection( baseUrl )
dbName  = "status-entities"

def findOUSStatus( ousID ):
    "Find an OUSStatus with the given ID, create a new one if none are found"

    retcode,ousStatus = dbcon.findOne( dbName, ousID )
    if retcode == 404:
        # Prepare a new record to write
        ousStatus = {}
        ousStatus['entityId'] = ousID
    return ousStatus

def setField( ousID, fieldName, fieldValue ):
    "Set the value of a field of an OUSStatus"
    ousStatus = findOUSStatus( ousID )
    ousStatus[fieldName] = fieldValue
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
	

print(" [x] Awaiting RPC requests to XTSS" )
executor = Executor( 'localhost', 'xtss', xtss )
executor.run()
