#!/usr/bin/env python3

import sys
import argparse
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection

parser = argparse.ArgumentParser( description='' )
parser.add_argument( dest="groupName" )
args=parser.parse_args()

queue = MqConnection( "localhost", "msgq" )

queue.joinGroup( args.groupName, listener="listener.NA" )
queue.joinGroup( args.groupName, listener="listener.EA" )
queue.joinGroup( args.groupName, listener="listener.EU" )

msg = {}
msg['text'] = "all is fine"
queue.send( msg, "selector.*" )
queue.send( msg, "selector.NA" )