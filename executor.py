#!/usr/bin/env python3

from rmq_filter import Filter
import sys
import json

# Create a filter

# Parse the command line arguments, then create a filter
filter_type = 'CMD_REPLY'
args = Filter.arg_parser( "TBD", filter_type ).parse_args()
listen_to=args.listen.split( ',' )
filter = Filter( 'localhost', 'pipe', filter_type, listen_to=listen_to )

# A closure, returning the callback we're interested in
def generateCallback( filter ):

	def callback(ch, method, properties, body):
		print("body='%s'" % (body))
		msg=json.loads(body)
		msgID=msg['msgID']
		callerID=msg['callerID']
		arg0=msg['arg0']
		print( "msgID='%s'" % msgID )
		print( "callerID='%s'" % callerID )
		print( "arg0='%s'" % arg0 )

		# We return the arg we were given, doubled
		ret=arg0+arg0
		ret_addr=callerID + '.' + msgID
		filter.send( ret, ret_addr )

	return callback

print(' [*] Waiting for commands from %r. To exit press CTRL+C' % listen_to )
callback = generateCallback( filter )
filter.listen( callback )
