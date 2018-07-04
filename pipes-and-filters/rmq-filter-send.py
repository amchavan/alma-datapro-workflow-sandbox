#!/usr/bin/env python3

import sys
sys.path.insert(0, "../shared")
from msgq import Filter
import argparse

# Example SEND_ONLY filter.
# Receives a message on pipe on localhost, 


# Parse the command line arguments

arg_parser = argparse.ArgumentParser( "Send off a message with a selector" )
arg_parser.add_argument( '-send', required=True, help="Selector for sent messages" )
arg_parser.add_argument( 'message', help='Message to send')
args=arg_parser.parse_args()
send_to=args.send
msg=args.message

# Create a filter
filter = Filter( 'localhost', 'pipe', send_to=args.send )
filter.send( msg )
print(" [x] Sent %s: %s" % (args.send, args.message))
