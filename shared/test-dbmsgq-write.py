#!/usr/bin/env python3

from dbmsgq import MessageQueue
import argparse


parser = argparse.ArgumentParser( description='qqq' )
parser.add_argument( dest="body", help="TODO" )
args=parser.parse_args()

queue = MessageQueue( "localhost", "msgq" )
msg = {}
msg['param1'] = 1
msg['param2'] = "2"
queue.send( msg, args.body )