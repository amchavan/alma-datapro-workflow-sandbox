#!/usr/bin/env python3

from rmq_filter import Filter
import sys

# Parse the command line arguments
filter_type='SEND_RECEIVE'
arg_parser = Filter.arg_parser( "Pass messages from the pipe on to the next stage", filter_type )
args=arg_parser.parse_args()
listen_to=args.listen.split( ',' )
send_to=args.send

# Create a filter
filter = Filter( 'localhost', 'pipe', filter_type, listen_to=listen_to, send_to=send_to )

# A closure, returning the callback we're interested in
def generateCallback( filter ):

	def callback(ch, method, properties, body):
		new_body = (body+body).decode()
		print(" [x] forwarding: %s:%s" % (listen_to, new_body))
		filter.send( new_body )

	return callback

print(' [*] Waiting for messages matching %r, forwarding to %r. To exit press CTRL+C' % (listen_to,send_to) )
callback = generateCallback( filter )
filter.listen( callback )
