#!/usr/bin/env python3
import os
import sys
import argparse
import subprocess
import re
import shutil
#import json
#import datetime
#from collections import namedtuple
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection
from ngascon import NgasConnection
import dbdrwutils

# Implements the replicated cache

def unzip( zipfile, dir="." ):
    """
        Unzip a file in directory
    """
    # Remove any existing dir, then expand
    filename = os.path.join( dir, zipfile )
    dirname  = os.path.basename( filename )
    # HACK: os.path.basename() doesn't seem to work
    dirname  = filename[:-4]
    try:
        print( ">>> unzip: removing:", dirname )
        shutil.rmtree( dirname )
    except FileNotFoundError:
        pass # no-op

    completed = subprocess.run( [ "unzip", "-o", "-d", "weblogs", zipfile ], cwd=dir  )
    print( ">>> unzip: in:", dir, "subprocess returned:", completed )
    return completed.returncode

def replicate( source, destination ):
    "Unconditionally replicate a source file or directory to a destination dir"
    
    cmd = ["rsync"]
    if os.path.isdir( source ):
        # The source is a directory, we must do some extra work
        basename = os.path.basename( source )
        source += "/"
        cmd.append( "-r" )
        destination += ("/" + basename)
    cmd.append( source )
    cmd.append( destination )
    print( ">>> rsync: from:", source, "to:", destination )
    completed = subprocess.run( cmd )
    print( ">>> rsync: subprocess returned:", completed )
    return completed.returncode

def replicateIfNeeded( my_exec, name, cachedAt ):
    if cachedAt == my_exec:
        return

    # Need to get the Weblog file into our local cache: 
    # get hold of the remote source location
    location = None
    if cachedAt == "EA":
        location = args.eacache
    elif cachedAt == "EU":
        location = args.eucache
    elif cachedAt == "NA":
        location = args.nacache
    else:
        raise RuntimeError( "Unknown executive: " + cachedAt )
    if location == None:
        raise RuntimeError( "Unknown remote location for files cached at " + cachedAt )

    # Get the file or directory over here
    rep_from = os.path.join( location, name )
    rep_to   = args.lcache
    retcode  = replicate( rep_from, rep_to )
    if retcode != 0:
        raise RuntimeError( "replicate %s %s: failed: errno: %d" % (rep_from, rep_to, retcode) )

def processWeblog( my_exec, filename, cachedAt ):
    print( ">>> processWeblog: my_exec: %s, filename: %s, cachedAt: %s" % ( my_exec, filename, cachedAt ))

    # Bring the Weblog over to our local cache, if needed
    replicateIfNeeded( my_exec, filename, cachedAt )

    # Now expand the weblog
    retcode = unzip( filename, args.lcache )
    if retcode != 0:
        raise RuntimeError( "unzip %s %s: failed: errno: %d" % (filename, args.lcache, retcode) )

    # If we're at JAO: save the Weblog in NGAS (will
    # be replicated to the ARCs)
    if my_exec != "JAO":
        return
    weblogPathname = os.path.join( args.lcache, filename )
    retcode = ngas.put( weblogPathname )
    if retcode != 0:
        raise RuntimeError( "NGAS put %s: failed: errno: %d" % (weblogPathname, retcode) )
    print( ">>> saved weblog to NGAS:", filename )

def processProductsDir( my_exec, dirname, cachedAt ):
    print( ">>> processProductsDir: my_exec: %s, dirname: %s, cachedAt: %s" % ( my_exec, dirname, cachedAt ))
    # Bring the Weblog over, if needed
    replicateIfNeeded( my_exec, dirname, cachedAt )
    return

def callback( message ):
    """
        Expects the body of the request to be a JSON document:
            {"fileType":"weblog", "cachedAt":"%s", "name": "%s"}
        where 
            fileType is be one of "weblog", "productsdir", ...
            cachedAt is be one of EA, EU, JAO and NA
            name     is the file or directory name

        For instance:
        {
            "fileType":"productsdir", 
            "cachedAt":"EU", 
            "name": "2015.1.00657.S_2018_07_18T11_38_13.263"
        }
    """
    print( ">>> message:", message )
    request = dbdrwutils.jsonToObj( message )

    if request.fileType == "weblog":
        processWeblog( args.exec, request.name, request.cachedAt )
    elif request.fileType == "productsdir":
        processProductsDir( args.exec, request.name, request.cachedAt )
    else:
        raise RuntimeError( "Unsupported fileType: " + request.fileType )

    return None


###################################################################
## Main program
###################################################################

parser = argparse.ArgumentParser( description='Replicated cache' )
parser.add_argument( "--exec",       "-e",    dest="exec",     help="Where this cache driver is running: one of 'EA', 'EU', 'JAO' or 'NA'" )
parser.add_argument( "--eacache",    "-eac",  dest="eacache",  help="Absolute pathname or rsync location of the EA cache dir" )
parser.add_argument( "--nacache",    "-nac",  dest="nacache",  help="Absolute pathname or rsync location of the NA cache dir" )
parser.add_argument( "--eucache",    "-euc",  dest="eucache",  help="Absolute pathname or rsync location of the EU cache dir" )
parser.add_argument( "--lcache",     "-lc",   dest="lcache",   help="Absolute pathname of the local cache dir" )
parser.add_argument( "--port",       "-p",    dest="port",     help="Port number of the Web server, dafault is 8000", default=8000 )
args=parser.parse_args()

listen_to = "cached." + args.exec
port = int(args.port)
mq = MqConnection( 'localhost', 'msgq', listen_to )
ngas = NgasConnection()
dbdrwutils.bgRunHttpServer( port, args.lcache )
print(' [*] Waiting for messages matching %s' % (listen_to) )
mq.listen( callback )

