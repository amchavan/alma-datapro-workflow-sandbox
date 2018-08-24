#!/usr/bin/env python3

import argparse
import sys
sys.path.insert(0, "../shared")
from dbmsgq import ExecutorClient

###################################################################
## Send an RPC request to an Executor
## Example:
##  ./send-rpc.py xtss \
##      '{ "operation":"get-ouss-by-state-substate", "state":"ReadyForProcessing", "substate":"^Pipeline" }'
##

parser = argparse.ArgumentParser( description='Temporary component, sends an arbitrary RPC request' )
parser.add_argument( dest="selector", help="Selector to send the message to" )
parser.add_argument( dest="message",  help="Message (JSON text)" )
args = parser.parse_args()

executor = ExecutorClient( 'localhost', 'msgq', args.selector )
response = executor.call( args.message )
print( "Response:", response )