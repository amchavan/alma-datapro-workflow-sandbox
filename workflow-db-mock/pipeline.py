#!/usr/bin/env python3

import argparse
import sys
import random
import datetime
import os
import tempfile
import shutil
import subprocess

# Mock-up of the Pipeline
# When invoked with an OUS ID it wait for a bit to simulate processing, then
# 	If all goes well creates a products directory and exits with a zero code
#   Otherwise it exits with a non-zero exit code and creates no directories
# Whether processing is OK or not is decided randomly
#
# 
# Maurizio Chavan, 16-Jul-2018

tempDir = tempfile.mkdtemp( prefix="drw-" )


def zipDirectory( dirname, workingDirectory ):
    zipfile = dirname + ".zip"
    completed = subprocess.run( ["zip", "-r", zipfile , dirname], cwd=workingDirectory  )
    pathname = os.path.join( workingDirectory, zipfile )
    return completed.returncode, pathname

def plProductsDirname( progID ):
	now = datetime.datetime.utcnow().isoformat()[:-3]
	dirname = progID + "_" + now
	dirname = dirname.replace( ":", "_" ).replace( "/", "_" ).replace( "-", "_" )
	dirname = dirname + "/SOUS/GOUS/MOUS/products"
	print( ">>> dirname: ", dirname )
	return dirname,now

def makeWeblog( progID, ousID, timestamp ):
	""" 
		Create weblog HTML file, return its absolute pathname.
		Basename starts with "weblog-" and ends with ".zip"
	"""
	htmlTemplate = """
	<!doctype html>
	<html>
		<head>
		  <title>Weblog</title>
		  <style>body {font-family: Verdana}</style>
		</head>
		<body>
		  <h1>Weblog for %s</h1>
		  <table>
		  <tr><td>OUS UID</td>   <td>%s</td></tr>
		  <tr><td>Prog ID</td>   <td>%s</td></tr>
		  <tr><td>Timestamp</td> <td>%s</td></tr>
		  </table>
		  (stuff goes here)
		</body>
	</html>
	"""
	html = htmlTemplate % (ousID, ousID, progID, timestamp)

	weblogBasedir = "weblog-%s-%s" % (ousID,timestamp)
	weblogBasedir = weblogBasedir.replace( "uid://", "" ).replace( "/", "-" )
	weblogDir = os.path.join( tempDir, weblogBasedir )
	weblogIndex = os.path.join( weblogDir, "index.html" )
	os.makedirs( weblogDir )
	with open( weblogIndex, 'w') as text_file:
		text_file.write( html )
	print( ">>> Weblog index.html:", weblogIndex )
	retcode,zipfile = zipDirectory( weblogBasedir, tempDir )
	print( ">>> zipfile:", zipfile )
	return zipfile

def makePipelineReport( progID, ousID, timestamp ):

	# Create Pipeline report XML file
	plReportTemplate = """
<?xml version="1.0" ?>
<PipelineAquaReport>
  <ProjectStructure>
    <ProposalCode>%s</ProposalCode>
    <OusStatusEntityId>%s</OusStatusEntityId>
  </ProjectStructure>
  <QaSummary>
    <ReportDate>%s</ReportDate>
    <ProcessingTime>17:15:00</ProcessingTime>
    <CasaVersion>5.1.1-5</CasaVersion>
    <PipelineVersion>40896M (Pipeline-CASA51-P2-B)</PipelineVersion>
    <FinalScore>Undefined</FinalScore>
  </QaSummary>
  <QaPerTopic>
    <Dataset     Score="1.0"></Dataset>
    <Flagging    Score="0.968228015835"></Flagging>
    <Calibration Score="0.964085631821"></Calibration>
    <Imaging     Score="0.999835593319"></Imaging>
  </QaPerTopic>
</PipelineAquaReport>
	"""
	plReport = plReportTemplate % (progID, ousID, timestamp.replace( 'T', ' ' ))
	plReportFile = "pl-report-%s-%s.xml" % (ousID,timestamp)
	plReportFile = plReportFile.replace( "uid://", "" ).replace( "/", "-" )
	plReportFile = os.path.join( tempDir, plReportFile )
	with open( plReportFile, 'w') as text_file:
		text_file.write( plReport )
	print( ">>> plReport:", plReportFile )
	return plReportFile

def makeDataProduct( ousID, timestamp, n ):

	# Create Pipeline data product file
	dataProd = "This is data product %d" % n
	dataProdFile = "product-%d-%s-%s.data" % (n,ousID,timestamp)
	dataProdFile = dataProdFile.replace( "uid://", "" ).replace( "/", "-" )
	dataProdFile = os.path.join( tempDir, dataProdFile )
	with open( dataProdFile, 'w') as text_file:
		text_file.write( dataProd )
	print( ">>> dataProd:", dataProdFile )
	return dataProdFile


###################################################################
## Main program
###################################################################

# TODO Pass in a processing recipe
parser = argparse.ArgumentParser( description='ALMA Pipeline mock-up' )
parser.add_argument( dest="progID", help="ID of the project containing the OUS" )
parser.add_argument( dest="ousID",  help="ID of the OUS that should be processed" )
parser.add_argument( dest="exec",   help="Executive where this pipeline is running" )
args=parser.parse_args()
ousID = args.ousID
progID = args.progID
executive = args.exec

print(" [x] Launching Pipeline on OUS %s, program %s, directory %s (%s)" % (ousID, progID, os.getcwd(), executive))

# Pretend we're doing something
waitTime = random.randint(3,15)
# sleep( waitTime )

# Simulate that there may be a processing problem
r = random.randint(1,100)
failed = (True if (r<=10) else False) # 10% chance of failing
if failed:
	shutil.rmtree( tempDir )
	sys.exit( 2 )	# exit with retcode 2 for "processing problem"

# No processing problem, create a products directory
dataProdsDir,timestamp = plProductsDirname( progID )
if not os.path.exists( dataProdsDir ):
    os.makedirs( dataProdsDir )

# Create a weblog in the products directory
weblog = makeWeblog( progID, ousID, timestamp )
os.rename( weblog, os.path.join( dataProdsDir, os.path.basename( weblog )))

# Create a pipeline report in the products directory
plReport = makePipelineReport( progID, ousID, timestamp )
os.rename( plReport, os.path.join( dataProdsDir, os.path.basename( plReport )))

# Create data products in the products directory
for i in [0,1,2,3,4]:
	dataProduct = makeDataProduct( ousID, timestamp, i )
	os.rename( dataProduct, os.path.join( dataProdsDir, os.path.basename( dataProduct )))

# All was OK
shutil.rmtree( tempDir )
exit( 0 )

