#!/usr/bin/env python3

from msgq import Filter
import sys

# Log all messages from pipe on localhost

# Create a filter
filter = Filter( 'localhost', 'pipe', listen_to='#' )

def callback(ch, method, properties, body):
    print(" [x] %s:%s" % (method.routing_key, body.decode()))

print(' [*] Logging messages matching %r. To exit press CTRL+C' % '#' )
filter.listen( callback )
