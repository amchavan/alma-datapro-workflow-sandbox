#!/usr/bin/env python3

import sys
sys.path.insert(0, "../shared")
import ngascon

def getTimestamp( productsDirName ):
	'''
		If productsDirName is something like '2015.1.00657.S_2018_07_19T08_50_10.228' 
		will return 2018-07-19T08:50:10.228
	'''
	n = productsDirName.index( '_' )
	print( n )
	timestamp = productsDirName[n+1:]
	timestamp = timestamp.replace( '_', '-', 2 )
	timestamp = timestamp.replace( '_', ':' )
	return timestamp

path = 's2015.1.00657.S_2018_07_19T08_50_10.228'
print( getTimestamp( path ))