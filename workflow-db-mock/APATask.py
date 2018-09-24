import time
from PLDriver import Task

import os
import argparse
import subprocess
import datetime
import tempfile
import shutil
import sys
import json
import base64
import time
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
import dbdrwutils
from ngascon import NgasConnection
from PLDriver import PLDriver

#Specific APAFunc class inheriting from Task.
#Pre or post tasks for the pipeline should be implemented in execute method.
class APAFunc(Task):
    def __init__(self):
        self._mq   = MqConnection( 'localhost', 'msgq' )
        self._xtss = ExecutorClient( 'localhost', 'msgq', 'xtss' )

    def execute(self, params):
        self._params = params
        #Check parameters!
        self._check(params, ['progID', 'ousUID', 'location', 'pipelineRunDir', 'replicatedCache'])
        self._progID = params['progID']
        self._ousUID = params['ousUID']
        self._pipelineRunDirectory = params['pipelineRunDir']
        self._replicatedCache = params['replicatedCache']
        self._location = params['location']

        # Copy the products directory to the replicating cache directory
        # and signal that to the JAO cache
        productsDir = self._findProductsDir(self._progID)
        productsBasedir = os.path.basename(productsDir)
        repCacheDir = os.path.join(self._replicatedCache, productsBasedir)
        print(">>> PipelineDriver: Products dir name:", productsDir)
        print(">>> PipelineDriver: Replicating dir name:", repCacheDir)
        self._copyAndReplaceDir(productsDir, repCacheDir)

        # message = '{"fileType":"productsdir", "cachedAt":"%s", "name": "%s"}' % (self._location,productsBasedir)
        message = {}
        message["fileType"] = "productsdir"
        message["cachedAt"] = self._location
        message["name"]     = productsBasedir
        selector = "cached.JAO"
        self._mq.send(message, selector)

        # Copy the weblog to the replicating cache directory
        # and signal that to the JAO *and* the local cache  (if
        # they are not one and the same)
        weblog = self._findWeblog(productsDir, self._ousUID)
        print(">>> PipelineDriver: weblog: copying", weblog, "to", self._replicatedCache)
        shutil.copy(weblog, self._replicatedCache)

        # message = '{"fileType":"weblog", "cachedAt":"%s", "name": "%s"}' % (self._location, os.path.basename(weblog))
        message = {}
        message["fileType"] = "weblog"
        message["cachedAt"] = self._location
        message["name"]     = os.path.basename(weblog)

        selector = "cached.JAO"
        self._mq.send(message, selector)
        if self._replicatedCache != "JAO":
            selector = "cached.%s" % self._location
            self._mq.send(message, selector)

        # Send the XML text of the pipeline report to AQUA at JAO
        # We need to BASE64-encode it because it will be wrapped in a JSON field
        timestamp = self._getTimestamp(productsBasedir)
        plReportFile = self._findPipelineReport(productsDir, self._ousUID)
        plReport = dbdrwutils.readTextFileIntoString(plReportFile)
        plReport = dbdrwutils.b64encode(plReport)
        message = '''
            {
                "ousUID" : "%s",
                "timestamp" : "%s",
                "source" : "%s",
                "report" : "%s",
                "productsDir": "%s"
            }
            ''' % (self._ousUID, timestamp, self._replicatedCache, plReport, productsBasedir)
        message = json.loads(message) # convert to a Python dict
        selector = "pipeline.report.JAO"
        self._mq.send(message, selector)

        # We are done, set the OUS state to ReadyForReview
        dbdrwutils.setState(self._xtss, self._ousUID, "ReadyForReview")

    def _isProductsDirectory(self, f, progID):
        return (not os.path.isfile(os.path.join(self._pipelineRunDirectory, f)) and f.startswith(progID))

    def _findProductsDir(self, progID):
        "Get the most recent product directory"
        allFiles = os.listdir(self._pipelineRunDirectory)
        prodDirs = [f for f in allFiles if self._isProductsDirectory(f, progID)]
        prodDir = sorted(prodDirs)[-1:]
        prodDir = prodDir[0]
        print(">>> PipelineDriver: prodDir:", prodDir)
        return os.path.join(self._pipelineRunDirectory, prodDir)

    def _findWeblog(self, productsDir, ousUID):
        # DEMO ONLY: the "products" subdirectory should be looked for
        #            here we just take the hardcoded path
        ousUID = dbdrwutils.encode(ousUID)
        productsDir = os.path.join(productsDir, "SOUS", "GOUS", ousUID, "products")
        for file in os.listdir(productsDir):
            print(">>> PipelineDriver: file:", file)
            if (file.startswith("weblog-") and file.endswith(".zip")):
                return (os.path.join(productsDir, file))
        raise RuntimeError("No weblog found in %s" % productsDir)

    def _findPipelineReport(self, productsDir, ousUID):
        # DEMO ONLY: the "products" subdirectory should be looked for
        #            here we just take the hardcoded path
        ousUID = dbdrwutils.encode(ousUID)
        productsDir = os.path.join(productsDir, "SOUS", "GOUS", ousUID, "products")
        for file in os.listdir(productsDir):
            print(">>> PipelineDriver: file:", file)
            if (file.startswith("pl-report-") and file.endswith(".xml")):
                return (os.path.join(productsDir, file))
        raise RuntimeError("No pipeline report found in %s" % productsDir)

    def _copyAndReplaceDir(self, from_path, to_path):
        if os.path.exists(to_path):
            shutil.rmtree(to_path)
        shutil.copytree(from_path, to_path)

    def _getTimestamp(self, productsDirName):
        '''
        If productsDirName is something like '2015.1.00657.S_2018_07_19T08_50_10.228'
        will return 2018-07-19T08:50:10.228
        '''
        n = productsDirName.index('_')
        timestamp = productsDirName[n+1:]
        timestamp = timestamp.replace('_', '-', 2)
        timestamp = timestamp.replace('_', ':')
        return timestamp
