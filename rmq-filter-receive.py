#!/usr/bin/env python3

from rmq_filter import Filter
import sys

# Log messages from the pipe on localhost: extract selectors and 
# message text from the command line

# Create a filter
filter = Filter( 'localhost', 'pipe', 'RECEIVE_ONLY' )

# Parse the command line arguments
arg_parser = filter.arg_parser( "Receive final messages from the pipe" )
args=arg_parser.parse_args()
selectors=args.listen.split( ',' )

def callback(ch, method, properties, body):
    print(" [x] %s: %s" % (method.routing_key, body.decode()))

print(' [*] Waiting for messages matching %r. To exit press CTRL+C' % selectors )
filter.listen( selectors, callback )
