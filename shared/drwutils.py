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

def broadcastStateChange( ousUID, state ):
    msg = "%s %s" % (ousUID, state)
    selector = stateChangeSelector % state
    sendMsgToSelector( msg, selector )

def broadcastRecipeChange( ousUID, recipe ):
    msg = "%s %s" % (ousUID, recipe)
    selector = recipeChangeSelector % recipe
    sendMsgToSelector( msg, selector )

def broadcastPipelineProcess( ousUID, executive, filter=filter ):
    msg = "%s" % ousUID
    selector = pipelineProcessSelector % executive
    sendMsgToSelector( msg, selector )

def setState( xtss, ousUID, state, broadcast=True ):
	"Set an OUS state via the XTSS"
	# Set the OUS's state to ReadyForProcessing
	request = "set-state %s %s" % (ousUID,state)
	print(" [x] Requesting %r" % request)
	response = xtss.call( request )
	print(" [.] response: %s" % response)
	if broadcast & (response == '201'):
	    broadcastStateChange( ousUID=ousUID, state=state )
