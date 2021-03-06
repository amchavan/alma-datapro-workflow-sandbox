import sys
import os
import subprocess
from collections import namedtuple
import json
import threading
import http.server, socketserver
import base64
import time
import datetime

#
# Utilities for the Data Reduction Workflow
#

__stateChange = "state.change"
stateChangeSelector = __stateChange + ".%s"
stateChangeListener = __stateChange + "..+"

# __recipeChange = "recipe.change"
# recipeChangeSelector = __recipeChange + ".%s"
# recipeChangeListener = __recipeChange + "..+"

def sendMsgToSelector( msg, selector, queue ):
    queue.send( msg, selector )
    print(" [x] Sent %r to %r" % (msg, selector))

def clearExecutive( xtss, ousUID ):
    "Clear an OUS executive via the XTSS"

    # request = '{"operation":"clear-exec", "ousUID":"%s", "value":""}' % (ousUID)
    request = {}
    request['operation'] = 'clear-exec'
    request['ousUID']    = ousUID
    request['value']     = None
    print(" [x] Requesting %r" % request)
    response = xtss.call( request )
    if response != 201:
        print(" [.] response: %s" % response)
    return response

def setExecutive( xtss, ousUID, executive ):
    "Set an OUS executive via the XTSS"

    # request = '{"operation":"set-exec", "ousUID":"%s", "value":"%s"}' % (ousUID, executive)
    request = {}
    request['operation'] = 'set-exec'
    request['ousUID']    = ousUID
    request['value']     = executive
    print(" [x] Requesting %r" % request)
    response = xtss.call( request )
    if response != 201:
        print(" [.] response: %s" % response)
    return response

def setState( xtss, ousUID, state ):
    "Set an OUS state via the XTSS"

    # request = '{"operation":"set-state", "ousUID":"%s", "value":"%s"}' % (ousUID,state)
    request = {}
    request['operation'] = 'set-state'
    request['ousUID']    = ousUID
    request['value']     = state
    print(" [x] Requesting %r" % request)
    response = xtss.call( request )
    if response != 201:
        print(" [.] response: %s" % response)
    return response

def setSubstate( xtss, ousUID, substate ):
    "Set an OUS substate via the XTSS"

    # request = '{"operation":"set-substate", "ousUID":"%s", "value":"%s"}' % (ousUID,substate)
    request = {}
    request['operation'] = 'set-substate'
    request['ousUID']    = ousUID
    request['value']     = substate
    print(" [x] Requesting %r" % request)
    response = xtss.call( request )
    if response != 201:
        print(" [.] response: %s" % response)
    return response

def zipDirectory( dirname, workingDirectory ):
    zipfile = dirname + ".zip"
    completed = subprocess.run( ["zip", "-r", zipfile, dirname], cwd=workingDirectory  )
    print( ">>> zipDirectory: subprocess returned:", completed )
    pathname = os.path.join( workingDirectory, zipfile )
    return completed.returncode, pathname

def readTextFileIntoString( filename ):
    with open(filename, mode='r') as file: 
        fileContent = file.read()
    return fileContent

def _json_object_hook( d ):
    "Service method for jsonToObj"
    return namedtuple('X', d.keys())(*d.values())

def jsonToObj( jsonString ):
    # See https://stackoverflow.com/questions/6578986/how-to-convert-json-data-into-a-python-object
    "Create a Python object from a JSON string"
    return json.loads( jsonString, object_hook=_json_object_hook )

def runHttpServer( port, resourceDir ):
    '''
        Runs a simple HTTP server.
        Parameters:
            port    TCP port the server should be listening to
            dir     directory from which resources are served
        After initialization, those resources are available from localhost:port.

        WARNING: will change the current working directory to "dir"
    '''
    Handler = http.server.SimpleHTTPRequestHandler
    os.chdir( resourceDir )
    httpd = socketserver.TCPServer(("", port), Handler)
    print( ">>> Started HTTP server on port", port)
    httpd.serve_forever()

def bgRunHttpServer( port, resourceDir ):
    '''
        Runs a simple HTTP server on a background thread.
        See runHttpServer for more info.
    '''
    bgRun( runHttpServer, (port,resourceDir) )

def bgRun( function, args=() ):
    '''
        Runs a function on a background thread.
        Parameter args should be a tuple including all
        the parameters to be passed to the function
    '''
    thread = threading.Thread( target=function, args=args )
    thread.start()

def b64encode( string ):
    return base64.b64encode( string.encode() ).decode()

def b64decode( string ):
    return base64.b64decode( string.encode() ).decode()

# Copied to pipeline.py -- keep that version in sync!
def makeWeblogName( ousUID, timestamp ):
    weblogName = "weblog-%s-%s" % (ousUID,timestamp)
    weblogName = weblogName.replace( "uid://", "" ).replace( "/", "-" )
    return weblogName

def nowISO():
    return datetime.datetime.utcnow().isoformat()[:-3]

def encode( entityID ):
    return entityID.replace( ":", "_" ).replace( "/", "_" ).replace( "-", "_" )

def incrementalSleep( startTime ):
    '''
        Returns the number of seconds to sleep waiting for an event to appear;
        sleep time increases with how long we've been waiting already
    '''
    now = time.time()
    waitingSince = now - startTime
    sleep = -1
    if waitingSince <= 60:
        # waiting since a minute or less: sleep for one sec
        sleep = 1
    elif waitingSince <= 300:
        # waiting since five minutes or less: sleep for 5 sec
        sleep = 5
    elif waitingSince <= 3600:
        # waiting since one hour or less: sleep for 30 sec
        sleep = 30
    else:
        # waiting since a long time: sleep for a minute
        sleep = 60
    # print( ">>> waitingSince: ", waitingSince, ", sleep for: ", sleep )
    return sleep


