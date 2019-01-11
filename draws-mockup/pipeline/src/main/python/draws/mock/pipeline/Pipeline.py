#!/usr/bin/env python3

import os
import sys
import time
import shutil
import random
import datetime
import tempfile
import argparse
import subprocess

# Note -- this script is made to run in an arbitrary directory
#         and it cannot depend on any library in 'shared'

# Mock-up of the Pipeline
# When invoked with an OUS ID it wait for a bit to simulate processing, then
# 	If all goes well creates a products directory and exits with a zero code
#   Otherwise it exits with a non-zero exit code and creates no directories
# Whether processing is OK or not is decided randomly
#
# 
# Maurizio Chavan, 16-Jul-2018

class Pipeline():
    def __init__(self, executive, ousUID, progID, recipe):
        self.tempDir = tempfile.mkdtemp(prefix="drw-")
        self.executive = executive
        self.ousUID = ousUID
        self.progID = progID
        self.recipe = recipe

    def start(self):
        print(">>> Pipeline: Launching on OUS %s, program %s, recipe %s, directory %s (%s)" % (self.ousUID, self.progID, self.recipe, os.getcwd(), self.executive))
        # Pretend we're doing something
        waitTime = random.randint(3,8)
        time.sleep(waitTime)
        # Simulate that there may be a processing problem
        r = random.randint(1,100)
        failed = (True if (r<=10) else False) # 10% chance of failing
        if failed:
        	shutil.rmtree(self.tempDir)
        	return 2 # exit with retcode 2 for "processing problem"
        # No processing problem, create a products directory
        dataProdsDir,timestamp = self.plProductsDirname(self.progID, self.ousUID)
        if not os.path.exists(dataProdsDir):
            os.makedirs(dataProdsDir)
        # Create a weblog in the products directory
        weblog = self.makeWeblog(self.progID, self.ousUID, timestamp)
        os.rename(weblog, os.path.join(dataProdsDir, os.path.basename(weblog)))
        # Create a pipeline report in the products directory
        plReport = self.makePipelineReport(self.progID, self.ousUID, timestamp)
        os.rename(plReport, os.path.join(dataProdsDir, os.path.basename(plReport)))
        # Create data products in the products directory
        for i in [0,1,2,3,4]:
        	dataProduct = self.makeDataProduct(self.ousUID, timestamp, i)
        	os.rename(dataProduct, os.path.join(dataProdsDir, os.path.basename(dataProduct)))
        # All was OK
        shutil.rmtree(self.tempDir)
        return 0

    def zipDirectory(self, dirname, workingDirectory):
        zipfile = dirname + ".zip"
        completed = subprocess.run(["zip", "-r", zipfile , dirname], cwd=workingDirectory)
        pathname = os.path.join(workingDirectory, zipfile)
        return completed.returncode, pathname

    def encode(self, entityID):
    	return entityID.replace(":", "_").replace("/", "_").replace("-", "_")
    
    def plProductsDirname(self, progID, ousUID):
    	now = datetime.datetime.utcnow().isoformat()[:-3]
    	dirname = self.encode(progID + "_" + now)
    	dirname = os.path.join(dirname, "SOUS", "GOUS", self.encode(ousUID), "products")
    	print(">>> Pipeline: products dirname: ", dirname)
    	return dirname,now
    
    # Copied from shared/dbdrwutils.py -- keep in sync!
    def makeWeblogName(self, ousUID, timestamp):
        weblogName = "weblog-%s-%s" % (ousUID,timestamp)
        weblogName = weblogName.replace("uid://", "").replace("/", "-")
        return weblogName
    
    def makeWeblog(self, progID, ousUID, timestamp):
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
    	html = htmlTemplate % (ousUID, ousUID, progID, timestamp)
    
    	weblogBasedir = self.makeWeblogName(ousUID, timestamp)
    	weblogDir = os.path.join(self.tempDir, weblogBasedir)
    	weblogIndex = os.path.join(weblogDir, "index.html")
    	os.makedirs(weblogDir)
    	with open(weblogIndex, 'w') as text_file:
    		text_file.write(html)
    	print(">>> Pipeline: Weblog index.html:", weblogIndex)
    	retcode,zipfile = self.zipDirectory(weblogBasedir, self.tempDir)
    	print(">>> Pipeline: zipfile:", zipfile)
    	return zipfile
    
    def makePipelineReport(self, progID, ousUID, timestamp):
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
    	plReport = plReportTemplate % (progID, ousUID, timestamp.replace('T', ' '))
    	plReportFile = "pl-report-%s-%s.xml" % (ousUID,timestamp)
    	plReportFile = plReportFile.replace("uid://", "").replace("/", "-")
    	plReportFile = os.path.join(self.tempDir, plReportFile)
    	with open(plReportFile, 'w') as text_file:
    		text_file.write(plReport)
    	print(">>> Pipeline: plReport:", plReportFile)
    	return plReportFile
    
    def makeDataProduct(self, ousUID, timestamp, n):
    	# Create Pipeline data product file
    	dataProd = "This is data product %d" % n
    	dataProdFile = "product-%d-%s-%s.data" % (n,ousUID,timestamp)
    	dataProdFile = dataProdFile.replace("uid://", "").replace("/", "-")
    	dataProdFile = os.path.join(self.tempDir, dataProdFile)
    	with open(dataProdFile, 'w') as text_file:
    		text_file.write(dataProd)
    	print(">>> Pipeline: dataProd:", dataProdFile)
    	return dataProdFile

if __name__ == "__main__":
    # Make sure we know where we are
    executive = os.environ.get('DRAWS_LOCATION')
    if executive == None:
        raise RuntimeError("DRAWS_LOCATION env variable is not defined")
    
    parser = argparse.ArgumentParser(description='ALMA Pipeline mock-up')
    parser.add_argument(dest="progID", help="ID of the project containing the OUS")
    parser.add_argument(dest="ousUID", help="ID of the OUS that should be processed")
    parser.add_argument(dest="recipe", help="Pipeline recipe to run")
    args=parser.parse_args()

    pipeline = Pipeline(executive, args.ousUID, args.progID, args.recipe)
    ret = pipeline.start()
    sys.exit(ret)
