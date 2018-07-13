#!/usr/bin/env python3

from dbmsgq import ExecutorClient
import argparse


parser = argparse.ArgumentParser( description='qqq' )
parser.add_argument( dest="n", help="TODO" )
args=parser.parse_args()

queue = ExecutorClient( "localhost", "msgq", "fibonacci" )
msg = {}
msg['n'] = args.n
print( "Executor returned:", queue.call( msg ))