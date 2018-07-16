import sys
sys.path.insert(0, "../shared")
from msgq import Filter

#
# Utilities for the Data Reduction Workflow
#

__stateChange = "state.change"
stateChangeSelector = __stateChange + ".%s"
stateChangeListener = __stateChange + ".*"

__recipeChange = "recipe.change"
recipeChangeSelector = __recipeChange + ".%s"
recipeChangeListener = __recipeChange + "..+"

__pipelineProcess = "pipeline.process"
pipelineProcessSelector = __pipelineProcess + ".%s"
pipelineProcessListener = __pipelineProcess + ".%s"

def sendMsgToSelector( msg, selector, queue ):
    queue.send( msg, selector )
    print(" [x] Sent %r to %r" % (msg, selector))

def broadcastStateChange( ousID, state, queue ):
    msg = "%s %s" % (ousID, state)
    selector = stateChangeSelector % state
    sendMsgToSelector( msg, selector, queue )

def broadcastRecipeChange( queue, ousID, recipe ):
    msg = "%s %s" % (ousID, recipe)
    selector = recipeChangeSelector % recipe
    sendMsgToSelector( msg, selector, queue )

def broadcastPipelineProcess( queue, ousID, executive ):
    msg = "%s" % ousID
    selector = pipelineProcessSelector % executive
    sendMsgToSelector( msg, selector, queue )

def setExecutive( xtss, ousID, executive ):
    "Set an OUS executive via the XTSS"

    request = "set-exec %s %s" % (ousID, executive)
    print(" [x] Requesting %r" % request)
    response = xtss.call( request )
    print(" [.] response: %s" % response)
    return response

def setState( xtss, ousID, state ):
    "Set an OUS state via the XTSS"

    request = "set-state %s %s" % (ousID,state)
    print(" [x] Requesting %r" % request)
    response = xtss.call( request )
    print(" [.] response: %s" % response)
    return response
