#!/usr/bin/env python3
import sys
sys.path.insert(0, "../shared")
from dbmsgq import Executor
from dbcon import DbConnection
import dbdrwutils
import datetime
import base64
import os

# TODO!

baseUrl = "http://localhost:5984" # CouchDB

class NgasConnection():
    '''
        A simple API for NGAS
    '''
    def __init__( self ):
        self.dbName  = "ngas"
        self.dbcon = DbConnection( baseUrl )

    def __readBinaryFileIntoString( self, filename ):
        with open(filename, mode='rb') as file: # b is important -> binary
            fileContent = file.read()
        s = base64.b64encode(fileContent).decode()
        return s

    def __writeStringAsBinaryFile( self, s, filename ):
        b = base64.b64decode( s )
        with open( filename, mode='wb' ) as file:
            file.write( b )

    def put( self, pathname ):
        s = self.__readBinaryFileIntoString( pathname )
        basename = os.path.basename( pathname )
        file = {}
        # file['filename'] = basename
        file['encodedContents'] = s
        file['writeTimestamp'] = dbdrwutils.nowISO()
        # print( ">>> attempting save(): dbName: %s, basename: %s, file: %s" % (self.dbName, basename, file) )
        retcode,msg = self.dbcon.save( self.dbName, basename, file )
        # print( ">>> ngas retcode:", retcode, "msg:", msg )
        return 0 if (retcode==201) else retcode
