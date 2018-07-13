#!/usr/bin/env python3

from dbmsgq import Executor

# Example Executor (RPC server)
# Will listen for requests on the 'pipe' queue 
# on localhost, the compute the N-th Fibonacci number,
# where N is the body of the request

def fib(n):
	# Make sure we can handle our input
    if( type(n) is str ):
        n = int(n)

    # Now do the usual Fibonacci recursion -- warning: will
    # run forever for n>35 or so
    if n == 0:
        return 0
    elif n == 1:
        return 1
    else:
        return fib(n-1) + fib(n-2)

def service( msg ):
	n=msg['n']
	return fib(n)

executor = Executor( "localhost", "msgq", "fibonacci", service )
executor.run()