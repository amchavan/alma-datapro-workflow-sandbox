#!/usr/bin/env python3

import sys
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
import argparse
import random
import dbdrwutils

# Mock-up of DRAssign

listen_to = dbdrwutils.recipeChangeListener
mq = MqConnection( 'localhost', 'msgq', listen_to )
xtss = ExecutorClient( 'localhost', 'msgq', 'xtss' )
# executives = ['EA', 'EU', 'JAO', 'NA']
executives = ['EU','EU','EU','EU']

# Business logic: assign to an Executive, then 
# request Pipeline processing

def callback( body ):
	# Body of the message is something like 
	#     uid://A003/X1/X3 PipelineCombination
	ousID = body.split()[0]
	executive = executives[random.randint(0,3)]
	response = dbdrwutils.setExecutive( xtss, ousID, executive )
	print(" [.] response: %r" % response)
	if response == 201:
	    dbdrwutils.broadcastPipelineProcess( mq, ousID, executive )

print(' [*] Waiting for messages matching %s' % (listen_to) )
mq.listen( callback )
