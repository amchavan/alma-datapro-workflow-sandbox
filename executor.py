#!/usr/bin/env python3

from rmq_filter import Filter
import sys
import json
import time

# Implements a simple command/reply executor. It listens for incoming
# commands and expects them to include a caller ID (to identify who
# sent the command), a message ID, a command name and a set of
# command-specific parameters.
# It will execute the command with those parameters, then return the
# command's result to the caller, including the command ID in the selector.
# It is the caller's responsibility to provide a meaningful command ID.
# In this example the command name is ignored.

# Parse the command line arguments, then create a filter
filter_type = 'CMD_REPLY'
args = Filter.arg_parser( "Example executor", filter_type ).parse_args()
listen_to=args.listen.split( ',' )
filter = Filter( 'localhost', 'exec-channel', filter_type, listen_to=listen_to )

# A closure, returning the callback we're interested in
def generateCallback( filter ):

	def callback(ch, method, properties, body):
		print(" [x] received: %s" % body )
		msg=json.loads(body)
		msgID=msg['msgID']
		callerID=msg['callerID']
		cmd=msg['cmd']					# IGNORED HERE
		arg0=msg['arg0']
		# print( "msgID='%s'" % msgID )
		# print( "callerID='%s'" % callerID )
		# print( "cmd='%s'" % cmd )
		# print( "arg0='%s'" % arg0 )

		# We return the arg we were given, doubled
		ret=arg0+arg0
		# we return to the original caller, including the message ID
		ret_addr=callerID + '.' + msgID
		time.sleep(2)	# wait a bit
		print(" [x] returning: %s:%s" % (ret_addr, ret))
		filter.send( ret, ret_addr )

	return callback

print(' [*] Waiting for commands from %r. To exit press CTRL+C' % listen_to )
callback = generateCallback( filter )
filter.listen( callback )
