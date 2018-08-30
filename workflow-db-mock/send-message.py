#!/usr/bin/env python3

import argparse
import sys
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection
import dbdrwutils

###################################################################
## Send an abitrary message to a selector/queue
## Example:
##  ./send-message.py pipeline.report.JAO \
##      ousUID=uid://X1/X1/Xaf source=EU report=Cjw/eG1sIHZlcnNpb2... timestamp=2018-07-19T08:50:10.228

parser = argparse.ArgumentParser( description='Sends an arbitrary message' )
parser.add_argument( dest="selector", help="Selector to send the message to" )
parser.add_argument( dest="keyValuePairs",  help="Message, key=value pairs", nargs='*' )
args = parser.parse_args()

message = {}
for keyValuePair in args.keyValuePairs:
	keyValue = keyValuePair.split( "=" )
	# print( keyValue )
	key = keyValue[0]
	value = keyValue[1]
	message[key] = value
	# print( key, "=", value )

# print( "Sending", message, "to", args.selector )

mq = MqConnection( 'localhost', 'msgq' )
dbdrwutils.sendMsgToSelector( message, args.selector, mq )