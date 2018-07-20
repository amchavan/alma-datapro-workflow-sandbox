#!/usr/bin/env python3

import sys
sys.path.insert(0, "../shared")
from msgq import Filter, ExecutorClient
import argparse
import random
import drwutils

# Mock-up of DRAssign

listen_to = [drwutils.recipeChangeListener]
filter = Filter( 'localhost', 'pipe', listen_to=listen_to )
xtss = ExecutorClient( 'localhost', 'xtss' )
executives = ['EA', 'EU', 'JAO', 'NA']

# DRAssign business logic: assign to an Executive, then 
# request Pipeline processing

def callback( ch, method, properties, body ):
	# Body of the message is something like 
	#     uid://A003/X1/X3 PipelineCombination
	ousUID = body.split()[0].decode("UTF-8")
	executive = executives[random.randint(0,3)]
	request = "set-exec %s %s" % (ousUID,executive)
	print(" [x] Requesting %s" % request)
	response = xtss.call( request )
	print(" [.] response: %r" % response)
	if response == '201':
	    drwutils.broadcastPipelineProcess( ousUID, executive, filter )

print(' [*] Waiting for messages matching %s' % (listen_to) )
filter.listen( callback )
