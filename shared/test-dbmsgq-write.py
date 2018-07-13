#!/usr/bin/env python3

from dbmsgq import MessageQueue

queue = MessageQueue( "localhost", "msgq" )
msg = {}
msg['param1'] = 1
msg['param2'] = "2"
queue.send( msg, "generrric" )