import sys
import os
import subprocess
from collections import namedtuple
import json
import threading
import http.server, socketserver
import base64
import datetime

#
# Utilities for the Data Reduction Workflow
#

__stateChange = "state.change"
stateChangeSelector = __stateChange + ".%s"
stateChangeListener = __stateChange + "..+"

__recipeChange = "recipe.change"
recipeChangeSelector = __recipeChange + ".%s"
recipeChangeListener = __recipeChange + "..+"

def sendMsgToSelector( msg, selector, queue ):
    queue.send( msg, selector )
    print(" [x] Sent %r to %r" % (msg, selector))

def broadcastStateChange( ousUID, state, queue ):
    msg = "%s %s" % (ousUID, state)
    selector = stateChangeSelector % state
    sendMsgToSelector( msg, selector, queue )

def broadcastRecipeChange( queue, ousUID, recipe ):
    msg = "%s %s" % (ousUID, recipe)
    selector = recipeChangeSelector % recipe
    sendMsgToSelector( msg, selector, queue )

def broadcastPipelineProcess( queue, progID, ousUID, recipe, executive ):
    msg = '{"progID":"%s", "ousUID":"%s", "recipe":"%s"}' % (progID, ousUID, recipe)
    selector = "pipeline.process.%s" % executive
    sendMsgToSelector( msg, selector, queue )

def setExecutive( xtss, ousUID, executive ):
    "Set an OUS executive via the XTSS"

    request = '{"operation":"set-exec", "ousUID":"%s", "value":"%s"}' % (ousUID, executive)
    print(" [x] Requesting %r" % request)
    response = xtss.call( request )
    print(" [.] response: %s" % response)
    return response

def setState( xtss, ousUID, state ):
    "Set an OUS state via the XTSS"

    request = '{"operation":"set-state", "ousUID":"%s", "value":"%s"}' % (ousUID,state)
    print(" [x] Requesting %r" % request)
    response = xtss.call( request )
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

def bgRun( function, args ):
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
    return datetime.datetime.now().isoformat()[:-3]

