import os
import sys
import time
import json
import base64
import shutil
import argparse
import datetime
import tempfile
import subprocess

from draws.messages.Publisher import Publisher
from draws.messages.RabbitMqMessageBroker import RabbitMqMessageBroker

from draws.mock.pldriver.Task import Task

from draws.mock.messages.gen.FileCache import FileCache
from draws.mock.messages.gen.XTSSSetState import XTSSSetState
from draws.mock.messages.gen.PLReport import PLReport

sys.path.insert(0, "../shared")
import dbdrwutils
from ngascon import NgasConnection


#Specific APAFunc class inheriting from Task.
#Pre or post tasks for the pipeline should be implemented in execute method.
class APAFunc(Task):
    def __init__(self):
        self._xtss = ExecutorClient( 'localhost', 'msgq', 'xtss' )
        self._broker = RabbitMqMessageBroker()

    def __del__(self):
        self._broker.closeConnection()

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

        message = FileCache("productsdir", self._location, productsBasedir)
        selector = "cached.JAO"
        cachedpub = Publisher(self._broker, selector)
        cachedpub.publish(message)

        # Copy the weblog to the replicating cache directory
        # and signal that to the JAO *and* the local cache  (if
        # they are not one and the same)
        weblog = self._findWeblog(productsDir, self._ousUID)
        print(">>> PipelineDriver: weblog: copying", weblog, "to", self._replicatedCache)
        shutil.copy(weblog, self._replicatedCache)

        message = FileCache("weblog", self._location, os.path.basename(weblog))
        selector = "cached.JAO"
        cachedpub = Publisher(self._broker, selector)
        cachedpub.publish(message)
        if self._replicatedCache != "JAO":
            selector = "cached.%s" % self._location
            cachedpub = Publisher(self._broker, selector)
            cachedpub.publish(message)

        # Send the XML text of the pipeline report to AQUA at JAO
        # We need to BASE64-encode it because it will be wrapped in a JSON field
        timestamp = self._getTimestamp(productsBasedir)
        plReportFile = self._findPipelineReport(productsDir, self._ousUID)
        plReport = dbdrwutils.readTextFileIntoString(plReportFile)
        plReport = dbdrwutils.b64encode(plReport)
        message = PLReport(self._ousUID, timestamp, self._replicatedCache, plReport, productsBasedir)
        selector = "pipeline.report.JAO"
        reportpub = Publisher(self._broker, selector)
        reportpub.publish(message)

        # We are done, set the OUS state to ReadyForReview
        #dbdrwutils.setState(self._xtss, self._ousUID, "ReadyForReview")
        setState = XTSSSetState(self._ousUID, "ReadyForReview")
        xtsspub = Publisher(self._broker, "xtss.transition")
        envelope = xtsspub.publish(setState)

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
