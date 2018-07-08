#!/usr/bin/env python3

import sys
sys.path.insert(0, "../shared")
from msgq import Filter

# Log all messages from pipe on localhost

# Create a filter
filter = Filter( 'localhost', 'pipe', listen_to='#' )

def callback(ch, method, properties, body):
    print(" [x] %s %s" % (method.routing_key, body.decode()))

print(' [*] Logging messages matching %r. To exit press CTRL+C' % '#' )
filter.listen( callback )
