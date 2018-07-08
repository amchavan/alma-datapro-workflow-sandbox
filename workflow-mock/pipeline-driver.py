#!/usr/bin/env python3

import sys
sys.path.insert(0, "../shared")
from msgq import Filter, ExecutorClient
import argparse
import random
import drwutils
from time import sleep

# Mock-up of the Pipeline Driver (successor of DARED)
# It pretends to be running at one of the executives

parser = argparse.ArgumentParser( description='Pipeline Driver mock-up' )
parser.add_argument( dest="exec", help="One of 'EA', 'EU', 'JAO' or 'NA'" )
args=parser.parse_args()

listen_to = [(drwutils.pipelineProcessListener % args.exec)]
filter = Filter( 'localhost', 'pipe', listen_to=listen_to )
xtss = ExecutorClient( 'localhost', 'xtss' )

# Pipeline Driver business logic: 
#    set the OUS state to Processing
#    launch the Pipeline
#    wait for the Pipeline to finish
#    set the OUS state to ProcessingProblem or ReadyForReview

def callback( ch, method, properties, body ):
	# Body of the message is something like 
	#     uid://A003/X1/X3 
	ousID = body.decode("UTF-8")
	drwutils.setState( xtss=xtss, ousID=ousID, state="Processing" )
	print(" [x] Launching Pipeline in on OUS %s in %s" % (args.exec,ousID) )
	# Wait for Pipeline to complete
	waitTime = random.randint(0,5)
	sleep( waitTime )

	# Simulate that there was a processing problem
	r = random.randint(1,100)
	failed = (True if (r<=50) else False) # 50% chance of failing
	if failed:
		drwutils.setState( xtss=xtss, ousID=ousID, state="ProcessingProblem" )
	else:
		drwutils.setState( xtss=xtss, ousID=ousID, state="ReadyForReview" )

	# request = "set-exec %s %s" % (ousID,executive)
	# print(" [x] Requesting %s" % request)
	# response = xtss.call( request )
	# print(" [.] response: %r" % response)
	# if response == '201':
	#     drwutils.broadcastPipelineProcess( ousID, executive, filter )

print(' [*] Waiting for messages matching %s' % (listen_to) )
filter.listen( callback )
