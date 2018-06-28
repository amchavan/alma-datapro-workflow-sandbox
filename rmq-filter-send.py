#!/usr/bin/env python3

from rmq_filter import Filter
import sys

# Send a message to the pipe on localhost: extract selector and 
# message text from the command line

# Create a filter
filter = Filter( 'localhost', 'pipe', 'SEND_ONLY' )

# Parse the command line arguments
arg_parser = filter.arg_parser( "Send off a message with a selector" )
arg_parser.add_argument( 'message', help='Message to send')
args=arg_parser.parse_args()
selector=args.send
msg=args.message

filter.send( selector, msg )
print(" [x] Sent %s: %s" % (selector, msg))
