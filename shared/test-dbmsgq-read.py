#!/usr/bin/env python3

from dbmsgq import MessageQueue

queue = MessageQueue( "localhost", "msgq" )

def myCallback( msg ):
	print( ">>> Received message:", msg )

queue.listen( myCallback, selectors="gene.+c" )