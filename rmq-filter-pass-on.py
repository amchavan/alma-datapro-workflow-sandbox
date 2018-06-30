#!/usr/bin/env python3

from msgq import Filter
import argparse
import sys

# Example SEND_RECEIVE filter.
# Receive a message on pipe on localhost, double it (concatenate the
# string with itself) and pass it on to another filter

# Parse the command line arguments
arg_parser = argparse.ArgumentParser( "Pass messages from the pipe on to the next stage" )
arg_parser.add_argument( '-listen', required=True, help="A comma-separated list of selectors for incoming messages" )
arg_parser.add_argument( '-send',   required=True, help="Selector for sent messages" )
args=arg_parser.parse_args()
listen_to=args.listen.split( ',' )
send_to=args.send

# Create a filter
filter = Filter( 'localhost', 'pipe', 'SEND_RECEIVE', listen_to=listen_to, send_to=send_to )

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
