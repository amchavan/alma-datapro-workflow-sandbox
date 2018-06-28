#!/usr/bin/env python3

from rmq_filter import Filter
import sys

# Create a filter
filter = Filter( 'localhost', 'pipe', 'SEND_RECEIVE' )

# Parse the command line arguments
arg_parser = filter.arg_parser( "Pass messages from the pipe on to the next stage" )
args=arg_parser.parse_args()
selectors=args.listen.split( ',' )
send=args.send

# A closure, returning the callback we're interested in
def generateCallback( filter ):

	def callback(ch, method, properties, body):
		new_body = (body+body).decode()
		print(" [x] forwarding: %s: %s" % (send, new_body))
		filter.send( send, new_body )

	return callback

print(' [*] Waiting for messages matching %r, forwarding to %r. To exit press CTRL+C' % (selectors,send) )
callback = generateCallback( filter )
filter.listen( selectors, callback )
