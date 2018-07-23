#!/usr/bin/env python3

from flask import Flask
import sys
sys.path.insert(0, "../../shared")
from dbcon import DbConnection
import json

# TODO

app = Flask( __name__ )

baseUrl = "http://127.0.0.1:5984" # CouchDB
dbCon   = DbConnection( baseUrl )
dbName  = "status-entities"
states  = [ "FullyObserved", "ReadyForProcessing", "Processing", 
			"ProcessingProblem", "ReadyForReview", 
			"Reviewing", "Verified",
			"DeliveryInProgress", "Delivered" ]
htmlTemplate = """
	<!doctype html>
	<html>
		<head>
		  <meta http-equiv="refresh" content="1" >
		  <title>Dashboard</title>
		  <style>
		  	body { font-family: Verdana }
		  	table{ width: 100%%; table-layout: fixed; }
			th { text-align: left; }
			td { padding: 5px; }
			.smalltext { font-size: smaller }
		  </style>
		</head>
		<body>
		  <h1>Dashboard</h1>
		  <h2>ObsUnitSets</h2>
		  <table>
		  %s
		  </table>
		</body>
	</html>
	"""

def compareByState( ous ):
	return ous['state']

def compareByTimestamp( ous ):
	return ous['timestamp']

def renderTable( table, rows, cols ):

	html = "<tr>"
	for state in states:
		html += ("<th>%s</th>\n" % state)
	html += "</tr>\n"
	
	for row in range(1, rows):
		html += "<tr>"
		for col in range( 0, cols ):
			t = table[row][col]
			ousUID,timestamp = t if (t != None) else ("&nbsp;","&nbsp;")
			timestamp = timestamp.replace( 'T', ' ' )
			html += ('<td><span>%s</span> <span class="smalltext">%s</span></td>' % (ousUID,timestamp))
		html += "</tr>\n"

	return html

def doDashboard( ousStatuses ):
	ousStatuses = sorted( ousStatuses, key=compareByTimestamp, reverse=True )
	ousStatuses = sorted( ousStatuses, key=compareByState )

	# Start building the table
	table = []
	headings = []
	for state in states:
		headings.append( state )
	table.append( headings )

	# Now feed the ousStatus entities to the table
	currentState = None
	row = 0
	rows = 0
	numStates = len( states )
	for ousStatus in ousStatuses:
		state = ousStatus['state']
		#print( "ousStatus = %s" % (ousStatus))
		#print( "state = %s" % (state))
		column = states.index( state )
		if state != currentState:
			row = 1
		else:
			row = row + 1
		if row > rows:
			table.append( [None] * numStates )
			rows += 1
		#print( "before: table = %s" % (table))
		#print( "        state = %s" % (state))
		#print( "table[%d][%d] = %s" % (row,column,state))
		table[row][column] = (ousStatus['entityId'], ousStatus['timestamp'])
		print( "after: table = %s" % (table))
		currentState = state

	ousTable = renderTable( table, rows+1, numStates )

	return htmlTemplate % ousTable


@app.route("/")
def dashboard():
	retcode,ousStatuses = dbCon.find( dbName, {"selector": { "state": { "$ne" : "" }}} )
	if retcode == 200:
		return doDashboard( ousStatuses )
	else:
		return "Error: %D" % retcode

if __name__ == "__main__":
	app.run()
