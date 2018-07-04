#!/usr/bin/env python3

import argparse
import sys
sys.path.insert(0, "../shared")
from msgq import ExecutorClient

# Example Executor client (RPC client)
# Will send requests to the 'pipe' queue 
# on localhost, passing our command line 
# positional arg 1 as N

parser = argparse.ArgumentParser( description='Fibonacci RPC client' )
parser.add_argument( dest="n", help="Fibonacci number" )
args=parser.parse_args()

fibonacci = ExecutorClient( 'localhost', 'pipe' )

print(" [x] Requesting fib(%s)" % args.n)
response = fibonacci.call( args.n )
print(" [.] Got %r" % response)
