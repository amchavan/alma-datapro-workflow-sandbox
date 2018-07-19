#!/usr/bin/env python3

from dbmsgq import MessageQueue

queue = MessageQueue( "localhost", "msgq", listenTo="^gener+ic$" )

def myCallback( msg ):
	print( ">>> Received message:", msg )

queue.listen( myCallback )