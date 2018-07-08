#!/usr/bin/env python3

import argparse
import sys
sys.path.insert(0, "../shared")
from msgq import ExecutorClient
import random
import drwutils

# Mock-up of AQUA-QA0, used to inject ReadyForProcessing OUSs into the workflow
# It also assumes the role of Cycle5 PLChecker, setting the Pipeline recipe

parser = argparse.ArgumentParser( description='AQUA QA0 mock-up' )
parser.add_argument( dest="uid", help="UID of an OUS that's become ReadyForProcessing" )
args=parser.parse_args()

xtss = ExecutorClient( 'localhost', 'xtss' )
recipes = ['PipelineCalibration','PipelineImaging',
		   'PipelineSingleDish', 'PipelineCombination', 
		   'PipelineCalAndImg']

# Set the OUS's state to ReadyForProcessing
state = "ReadyForProcessing"
request = "set-state %s %s" % (args.uid,state)
print(" [x] Requesting %r" % request)
response = xtss.call( request )
print(" [.] response: %s" % response)
if response == '201':
    drwutils.broadcastStateChange( ousID=args.uid, state=state )

# Set the Pipeline recipe to a random one
recipe = recipes[random.randint(0,4)]
request = "set-recipe %s %s" % (args.uid,recipe)
print(" [x] Requesting %s" % request)
response = xtss.call( request )
print(" [.] response: %s" % response)
if response == '201':
    drwutils.broadcastRecipeChange( ousID=args.uid, recipe=recipe )
