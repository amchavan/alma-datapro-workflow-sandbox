#!/usr/bin/env python3

from rmq_filter import Filter
import argparse
import sys

# Send a message to the pipe on localhost: extract executor and
# message text from the command line

parser = argparse.ArgumentParser( description='Client of the executor' )
parser.add_argument('-executor', help="ID of the executor", required=True )
parser.add_argument( 'message',  help='Message to send')
args=parser.parse_args()
executor=args.executor
message=args.message

print(" [x] args %s: %s" % (executor, message))

# We will listen to messages sent by the executor back to us, including
# the message ID
callerID="me"
messageID="msg1"
cmd="double"

# Now send a command to the executor
json='{"callerID":"%s","msgID":"%s","cmd":"%s","arg0":"%s"}' % (callerID,messageID, cmd, message)
Filter.send_msg( 'localhost', 'exec-channel', executor, json )
print(" [x] sent %s: %s" % (executor, json))

def generateExecutorCallback():
    """
        Return the callback to be invoked when the executor
        gets back to us with a result -- just print out what we got
    """

    def callback(ch, method, properties, body):
        print(" [x] got: %s:%s" % (method.routing_key, body.decode()))

    return callback

# Get ready for the executor to get back to us
selector = "%s.*" % callerID
print(" [x] listening on %s" % selector)
Filter.listen_in( 'localhost', 'exec-channel', [selector], generateExecutorCallback() )
