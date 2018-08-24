#!/usr/bin/env python3

from flask import Flask
import sys
sys.path.insert(0, "../../shared")
from dbcon import DbConnection
import json
from flask import Flask, request, send_from_directory

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
		  <meta http-equiv="refresh" content="1" >	<!-- Refresh every few secs -->
		  <title>Dashboard</title>
		  <style>
		  	body { font-family: Verdana }
			th { text-align: left; }
			td { padding: 5px; }
			caption { font-size: larger; font-style: italic; font-weight: bold;}

		  	.wide-table{ width: 100%%; table-layout: fixed; }
		  	.tables div { float: left; }
		  	.table-div  { border-width:1px; border-style:solid; border-color:gray; margin: 5px; }
			.smalltext { font-size: smaller }
		  </style>
		</head>
		<body>
		  <h1>Dashboard</h1>
		  <div>
		  	<ul>
				<li> <a href="http://localhost:8000/"        target="_blank"><!-- <img src="resources/cache.png"> --> Local cache</a>
		  		<li> <a href="http://localhost:8000/weblogs" target="_blank"><!-- <img src="resources/cache.png"> --> Locally cached weblogs</a> 
	  		</ul>
		  </div>
		  <div class="table-div">
		  <table class="wide-table">
		  	<caption>ObsUnitSets status entities</caption>
		  %s
		  </table>
		  </div>
		  	<div class="tables">
				<div class="table-div">
					<table>
					  	<caption>Pipeline reports</caption>
					  	%s
					</table>
				</div>
				<div class="table-div">
					<table>
					  	<caption>Delivery status</caption>
					  	%s
					</table>
				</div>
			  	<div class="table-div">
				  	<table>
					  	<caption>Unread messages</caption>
					  	%s
				  	</table>
				</div>
			</div>
			<div>
				<!-- Create an empty wide table to break the floating and start a on a new row -->
			 	<table class="wide-table"></table>
			</div>
			<div class="tables">
			  <div class="table-div">
			  	<table>
				  	<caption>NGAS documents</caption>
				  	%s
			  	</table>
			  </div>
			  <div class="table-div">
			  	<table>
				  	<caption>Products metadata</caption>
				  	%s
			  	</table>
			  </div>
			</div>
		</body>
	</html>
	"""

def compareByState( record ):
	return record['state']

def compareByOusUid( record ):
	return record['ousUID']

def compareById( record ):
	return record['_id']

def compareByTimestamp( record ):
	return record['timestamp']

def compareByWriteTimestamp( record ):
	return record['writeTimestamp']

def compareByCreationTimestamp( record ):
	return record['creationTimestamp']

def renderOusTable( table, rows, cols ):
	htmlCellTemplate = '<td> %s %s %s </td>'

	html = "<tr>"
	for state in states:
		html += ("<th>%s</th>\n" % state)
	html += "</tr>\n"
	
	for row in range(1, rows):
		html += "<tr>"
		for col in range( 0, cols ):
			t = table[row][col]
			ousUID,timestamp,substate,exec = t if (t != None) else ("&nbsp;","&nbsp;",None, "&nbsp;")
			timestamp = timestamp.replace( 'T', '&nbsp;' )

			ousUIDSpan    = '<span>%s</span>' % ousUID
			timestampSpan = '<span class="smalltext">%s</span>' % timestamp
			substateSpan  = ('<span class="smalltext">%s %s</span>' % (substate,exec)) if substate else ""

			html += (htmlCellTemplate % (ousUIDSpan,substateSpan,timestampSpan))
		html += "</tr>\n"

	return html

def doOusStatusTable( ousStatuses ):
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
		exec = extractExec( ousStatus )
		table[row][column] = (ousStatus['entityId'], ousStatus['timestamp'], ousStatus['substate'], exec )
		# print( "after: table = %s" % (table))
		currentState = state

	ousTable = renderOusTable( table, rows+1, numStates )
	return ousTable

def extractExec( ousStatus ):
	flags = ousStatus['flags']
	flag = 'PL_PROCESSING_EXECUTIVE'
	if flag in flags:
		return flags[flag]
	else:
		return ''
		


def doPipelineReports( pipelineReports ):
	pipelineReports = sorted( pipelineReports, key=compareByTimestamp, reverse=True )

	table = []
	row = 0
	for pipelineReport in pipelineReports:
		table.append( [None] * 2 )
		table[row][0] = pipelineReport['ousUID']
		table[row][1] = pipelineReport['timestamp']
		row += 1

	html = renderPipelineReportsTable( table, len( pipelineReports ), 2 )
	return html

def renderPipelineReportsTable( table, rows, cols ):
	html = '<tr><th>OUS UID</th><th>Timestamp</th>\n'
	if rows > 0:
		for row in range(0, rows):
			ousUID    = table[row][0]
			timestamp = table[row][1]
			timestamp = timestamp.replace( 'T', '&nbsp;' )
			html 	  += "<tr><td>%s</td><td>%s</td></tr>\n" % (ousUID,timestamp)
	return html

def doNgasDocuments( ngasDocs ):
	ngasDocs = sorted( ngasDocs, key=compareByWriteTimestamp, reverse=True )

	table = []
	row = 0
	for ngasDoc in ngasDocs:
		table.append( [None] * 2 )
		table[row][0] = ngasDoc['_id']
		table[row][1] = ngasDoc['writeTimestamp']
		row += 1

	html = renderNgasDocuments( table, len( ngasDocs ), 2 )
	return html

def renderNgasDocuments( table, rows, cols ):
		
	html = '<tr><th>NGAS ID</th><th>Write timestamp</th>\n'

	if rows > 0:
		for row in range(0, rows):
			ousUID    = table[row][0]
			timestamp = table[row][1]
			timestamp = timestamp.replace( 'T', '&nbsp;' )
			html 	  += "<tr><td>%s</td><td>%s</td></tr>\n" % (ousUID,timestamp)
	return html

def doProductsMetadata( prodsMeta ):
	ngasDocs = sorted( prodsMeta, key=compareByTimestamp, reverse=True )

	table = []
	row = 0
	for prodMeta in prodsMeta:
		table.append( [None] * 4 )
		table[row][0] = prodMeta['_id']
		table[row][1] = prodMeta['timestamp']
		table[row][2] = prodMeta['progID']
		table[row][3] = prodMeta['ousUID']
		row += 1

	html = renderProductsMetadata( table, len( prodsMeta ), 4 )
	return html

def renderProductsMetadata( table, rows, cols ):
		
	html = '<tr><th>Product ID</th><th>Timestamp</th><th>Program ID</th><th>OUS UID</th>\n'

	if rows > 0:
		for row in range(0, rows):
			prodID    = table[row][0]
			timestamp = table[row][1]
			progID    = table[row][2]
			ousUID    = table[row][3]
			timestamp = timestamp.replace( 'T', '&nbsp;' )
			html 	  += "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>\n" % (prodID, timestamp, progID, ousUID)
	return html


def doDeliveryStatus( delStatuses ):
	ngasDocs = sorted( delStatuses, key=compareByTimestamp, reverse=True )

	table = []
	row = 0
	for delStatus in delStatuses:
		table.append( [None] * 4 )
		table[row][0] = delStatus['timestamp']
		table[row][1] = delStatus['progID']
		table[row][2] = delStatus['ousUID']
		table[row][3] = delStatus['complete']
		row += 1

	html = renderDeliveryStatus( table, len( delStatuses ), 4 )
	return html

def renderDeliveryStatus( table, rows, cols ):
		
	html = '<tr><th>Timestamp</th><th>Program ID</th><th>OUS UID</th><th>Delivered?</th>\n'
	if rows > 0:
		for row in range(0, rows):
			timestamp = table[row][0]
			progID 	  = table[row][1]
			ousUID    = table[row][2]
			complete  = table[row][3]
			timestamp = timestamp.replace( 'T', '&nbsp;' )
			html 	  += "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>\n" % (timestamp, progID, ousUID, complete)
	return html

def doUnreadMessages( unreadMessages ):
	ngasDocs = sorted( unreadMessages, key=compareByCreationTimestamp, reverse=True )
	table = []
	row = 0
	for unreadMessage in unreadMessages:
		table.append( [None] * 2 )
		table[row][0] = unreadMessage['selector']
		table[row][1] = unreadMessage['creationTimestamp']
		row += 1

	html = renderUnreadMessages( table, row, 4 )
	return html

def renderUnreadMessages( table, rows, cols ):
		
	html = '<tr><th>Selector</th><th>Timestamp</th>\n'
	if rows >= 0:
		for row in range(0, rows):
			selector  = table[row][0]
			timestamp = table[row][1]
			timestamp = timestamp.replace( 'T', '&nbsp;' )
			html 	  += "<tr><td>%s</td><td>%s</td></tr>\n" % (selector, timestamp)
	return html


def doDashboard( ousStatuses, pipelineReports, ngasDocs, prodsMeta, delStatuses, unreadMessages ):
	ousTable      = doOusStatusTable( ousStatuses )
	plTable       = doPipelineReports( pipelineReports )
	ngasTable     = doNgasDocuments( ngasDocs )
	prodMetaTable = doProductsMetadata( prodsMeta )
	delStatTable  = doDeliveryStatus( delStatuses )
	msgTable      = doUnreadMessages( unreadMessages )

	return htmlTemplate % (ousTable, plTable, delStatTable, msgTable, ngasTable, prodMetaTable )

@app.route('/resources/<path:path>')
def send_js(path):
    return send_from_directory( 'resources', path )

@app.route("/")
def dashboard():
	retcode,ousStatuses = dbCon.find( dbName, {"selector": { "state": { "$ne" : "" }}} )
	if retcode != 200:
		raise RuntimeError( "Error %d: %s" % retcode,ousStatuses )

	retcode,pipelineReports = dbCon.find( 'pipeline-reports', {"selector": { "ousUID": { "$ne": "" }}} )
	if retcode != 200:
		raise RuntimeError( "Error %d: %s" % retcode,ousStatuses )

	retcode,ngasDocs = dbCon.find( 'ngas', {"selector": { "_id": { "$ne": "" }}} )
	if retcode != 200:
		raise RuntimeError( "Error %d: %s" % retcode,ngasDocs )

	retcode,prodsMeta = dbCon.find( 'products-metadata', {"selector": { "_id": { "$ne": "" }}} )
	if retcode != 200:
		raise RuntimeError( "Error %d: %s" % retcode,prodsMeta )

	retcode,delStatuses = dbCon.find( 'delivery-status', {"selector": { "_id": { "$ne": "" }}} )
	if retcode != 200:
		raise RuntimeError( "Error %d: %s" % retcode,delStatuses )

	retcode,unreadMessages = dbCon.find( 'msgq', {"selector": { "consumed": False }} )
	if retcode != 200:
		raise RuntimeError( "Error %d: %s" % retcode,messages )

	return doDashboard( ousStatuses, pipelineReports, ngasDocs, prodsMeta, delStatuses, unreadMessages )
	
if __name__ == "__main__":
	app.run()

