#!/usr/bin/env python3

import argparse
import sys
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection
import dbdrwutils

###################################################################
## Main program
###################################################################

parser = argparse.ArgumentParser( description='Temporary component, sends an arbitrary message' )
parser.add_argument( dest="selector", help="Selector to send the message to" )
parser.add_argument( dest="message",  help="Message (JSON text)" )
args = parser.parse_args()
selector = args.selector
message = args.message

mq = MqConnection( 'localhost', 'msgq' )
dbdrwutils.sendMsgToSelector( args.message, args.selector, mq )