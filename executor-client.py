#!/usr/bin/env python3

import argparse
from msgq import ExecutorClient

# Example Executor client (RPC client)
# Will send requests to the 'fibonacci' queue 
# on localhost, passing our command line 
# positional arg 1 as N

parser = argparse.ArgumentParser( description='Fibonacci RPC client' )
parser.add_argument( dest="n", help="Fibonacci number" )
args=parser.parse_args()

fibonacci = ExecutorClient( 'localhost', 'fibonacci' )

print(" [x] Requesting fib(%s)" % args.n)
response = fibonacci.call( args.n )
print(" [.] Got %r" % response)
