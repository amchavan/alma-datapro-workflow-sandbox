import sys
import subprocess
import os
sys.path.insert(0, "../shared")
from msgq import Filter

#
# Utilities for the Data Reduction Workflow
#

filter = Filter( 'localhost', 'pipe' )

__stateChange = "state.change"
stateChangeSelector = __stateChange + ".%s"
stateChangeListener = __stateChange + ".*"

__recipeChange = "recipe.change"
recipeChangeSelector = __recipeChange + ".%s"
recipeChangeListener = __recipeChange + ".*"

__pipelineProcess = "pipeline.process"
pipelineProcessSelector = __pipelineProcess + ".%s"
pipelineProcessListener = __pipelineProcess + ".%s"

def sendMsgToSelector( msg, selector ):
    filter.send( msg, selector )
    print(" [x] Sent %r to %r" % (msg, selector))

def broadcastStateChange( ousID, state ):
    msg = "%s %s" % (ousID, state)
    selector = stateChangeSelector % state
    sendMsgToSelector( msg, selector )

def broadcastRecipeChange( ousID, recipe ):
    msg = "%s %s" % (ousID, recipe)
    selector = recipeChangeSelector % recipe
    sendMsgToSelector( msg, selector )

def broadcastPipelineProcess( ousID, executive, filter=filter ):
    msg = "%s" % ousID
    selector = pipelineProcessSelector % executive
    sendMsgToSelector( msg, selector )

def setState( xtss, ousID, state, broadcast=True ):
	"Set an OUS state via the XTSS"
	# Set the OUS's state to ReadyForProcessing
	request = "set-state %s %s" % (ousID,state)
	print(" [x] Requesting %r" % request)
	response = xtss.call( request )
	print(" [.] response: %s" % response)
	if broadcast & (response == '201'):
	    broadcastStateChange( ousID=ousID, state=state )
