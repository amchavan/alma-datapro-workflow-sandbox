#!/usr/bin/env python3

from msgq import Filter
import argparse
import sys

# Example RECEIVE_ONLY filter.
# Log messages from the pipe on localhost: extract selectors and
# message text from the command line

# Parse the command line arguments
arg_parser = argparse.ArgumentParser( "Receive final messages from the pipe" )
arg_parser.add_argument( '-listen', required=True, help="A comma-separated list of selectors for incoming messages" )
args=arg_parser.parse_args()
listen_to=args.listen.split( ',' )

# Create a filter
filter = Filter( 'localhost', 'pipe', 'RECEIVE_ONLY', listen_to=listen_to )

def callback(ch, method, properties, body):
    print(" [x] %s: %s" % (method.routing_key, body.decode()))

print(' [*] Waiting for messages matching %r. To exit press CTRL+C' % listen_to )
filter.listen( callback )
