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
    "Find an OUSStatus with the given ID, raise an error if none are found"

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

def setExecutive( ousUID, executive ):
    "Set the Executive of an OUSStatus"
    return setFlag( ousUID, 'PL_PROCESSING_EXECUTIVE', executive )

def clearExecutive( ousUID ):
    "Clear the Executive of an OUSStatus"
    return clearFlag( ousUID, 'PL_PROCESSING_EXECUTIVE' )

def setFlag( ousUID, name, value ):
    "Set an OUSStatus flag"
    ousStatus = findOUSStatus( ousUID )
    if 'flags' in ousStatus:
        flags = ousStatus['flags']
    else:
        flags = {}
    flags[name] = value
    return setField( ousUID, 'flags', flags )

def clearFlag( ousUID, name ):
    "Clear an OUSStatus flag"
    ousStatus = findOUSStatus( ousUID )
    if 'flags' in ousStatus:
        flags = ousStatus['flags']
    else:
        flags = {}
    if name in flags:
        del flags[name]
    return setField( ousUID, 'flags', flags )

def findByStateSubstate( state, substate ):
	"""
		Returns a return code and, if all was well and the code is 200, 
        all OUSs with the given state and substate; note substate is
        interpreted as a regexp
	"""
	selector = {
	   "selector": {
			"state": state,
			"substate": { "$regex": substate }
       }
	}
	retcode,ouss = dbcon.find( "status-entities", selector )
	if retcode == 200:
	    ouss.sort( key=lambda x: x['entityId'])
	return retcode,ouss



def xtss( body ):
    """
        Expects the body of the request to be a JSON document including field
        "operation". The name and value of other fields depend on 
        the operation itself. For instance:
            { 
                "operation":"set-state", 
                "ousUID":"uid://A003/X1/X1a", 
                "value":"ReadyForReview"
            }
        or:
            { 
                "operation":"get-ouss-by-state-substate", 
                "state":"ReadyForProcessing", 
                "substate":"^Pipeline"
            }

        For 'set' operations, returns 201 (created) if all was well.
        For 'get' operations, returns .
    """
    print(" [*] request: ", body )
    operation = body["operation"]

    if operation.startswith( "set-" ):
        ousUID = body["ousUID"]
        value = body["value"]

    if operation == "set-state":
        retcode = setState( ousUID=ousUID, state=value )
        return retcode

    elif operation == "set-substate":
        retcode = setSubstate( ousUID=ousUID, substate=value )
        return retcode

    elif operation == "set-exec":
        retcode = setExecutive( ousUID=ousUID, executive=value )
        return retcode

    elif operation == "clear-exec":
        ousUID = body["ousUID"]
        retcode = clearExecutive( ousUID=ousUID )
        return retcode

    elif operation == "get-ouss-by-state-substate":
        # Currently unused
        state = body["state"]
        substate = body["substate"]
        retcode,ouss = findByStateSubstate( state, substate )
        if retcode != 200:
            return retcode
        return ouss

    else:
        return "Unsupported operation: " + operation

    return None
	

executor = Executor( 'localhost', 'msgq', 'xtss', xtss )
print(" [x] Awaiting RPC requests to 'xtss'" )
executor.run()
