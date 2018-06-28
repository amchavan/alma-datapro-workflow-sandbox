#!/usr/bin/env python3

import argparse
import sys

parser = argparse.ArgumentParser(description='Short sample app')

parser.add_argument('-a', action="store_true", default=False)
parser.add_argument('-b', action="store", dest="b")
parser.add_argument('-c', action="store", dest="c", type=int, help='oho!')

args=parser.parse_args(  )
# args=parser.parse_args( ['-bv', '-c 1' ] )
print( args.a )
print( args.b )
print( args.c )

