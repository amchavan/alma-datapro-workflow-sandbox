#!/usr/bin/env python3

from rmq_filter import Filter
import sys

# Send a message to the pipe on localhost: extract selector and
# message text from the command line


# Parse the command line arguments
filter_type='SEND_ONLY'
arg_parser = Filter.arg_parser( "Send off a message with a selector", filter_type )
arg_parser.add_argument( 'message', help='Message to send')
args=arg_parser.parse_args()
send_to=args.send
msg=args.message

# Create a filter
filter = Filter( 'localhost', 'pipe', filter_type, send_to=send_to )

filter.send( msg )
print(" [x] Sent %s: %s" % (send_to, msg))
