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
##      '"ousUID" : "uid://X1/X1/Xaf",  "source" : "EU",  "report" : "Cjw/eG1sIHZlcnNpb2...",  "timestamp" : "2018-07-19T08:50:10.228", "productsDir": "2015.1.00657.S_2018_07_19T08_50_10.228"'
##

parser = argparse.ArgumentParser( description='Sends an arbitrary message' )
parser.add_argument( dest="selector", help="Selector to send the message to" )
parser.add_argument( dest="message",  help="Message (JSON text)" )
args = parser.parse_args()

mq = MqConnection( 'localhost', 'msgq' )
dbdrwutils.sendMsgToSelector( args.message, args.selector, mq )