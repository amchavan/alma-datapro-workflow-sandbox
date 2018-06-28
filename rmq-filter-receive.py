#!/usr/bin/env python3

from rmq_filter import Filter
import sys

# Log messages from the pipe on localhost: extract selectors and
# message text from the command line

# Parse the command line arguments
filter_type='RECEIVE_ONLY'
arg_parser = Filter.arg_parser( "Receive final messages from the pipe", filter_type )
args=arg_parser.parse_args()
listen_to=args.listen.split( ',' )

# Create a filter
filter = Filter( 'localhost', 'pipe', filter_type, listen_to=listen_to )

def callback(ch, method, properties, body):
    print(" [x] %s: %s" % (method.routing_key, body.decode()))

print(' [*] Waiting for messages matching %r. To exit press CTRL+C' % listen_to )
filter.listen( callback )
