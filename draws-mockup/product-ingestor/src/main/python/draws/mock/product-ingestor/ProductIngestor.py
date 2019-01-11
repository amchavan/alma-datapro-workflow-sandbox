#!/usr/bin/env python3

import sys
# import random
import argparse
import os
import time
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
from dbcon import DbConnection
from ngascon import NgasConnection
import dbdrwutils

# Mock-up of the Product Ingestor
# TODO

class ProductIngestor():
    def __init__(self, localCache):
        self.baseUrl = "http://localhost:5984" # CouchDB
        self.ngascon = NgasConnection()
        self.xtss = ExecutorClient('localhost', 'msgq', 'xtss')
        self.select = "ingest.JAO"
        self.mq = MqConnection('localhost', 'msgq',  self.select)
        self.localCache = localCache

    def start(self):
        print(' [*] Waiting for messages matching', self.select)
        self.mq.listen(self.callback)

    def encode(self, entityID):
        return entityID.replace(":", "_").replace("/", "_")
    
    def setSubstate(self, ousUID, substate):
        dbdrwutils.setSubstate(self.xtss, ousUID, substate)
    
    def writeMetadata(self, progID, ousUID, timestamp, dataProduct):
        # No error checking!
        dbcon  = DbConnection(self.baseUrl)
        dbName  = "products-metadata"
        metadata = {}
        metadata['progID'] = progID
        metadata['ousUID'] = ousUID
        metadata['timestamp'] = timestamp
        retcode,retmsg = dbcon.save(dbName, dataProduct, metadata)
        if retcode != 201:
            raise RuntimeError("setSubstate: error %d, %s" % (retcode,retmsg))
    
    def writeDeliveryStatus(self, progID, ousUID, timestamp, dataProducts, complete=False):
        # No error checking!
        dbcon  = DbConnection(self.baseUrl)
        dbName  = "delivery-status"
        delStatus = {}
        delStatus['progID'] = progID
        delStatus['timestamp'] = timestamp
        delStatus['dataProducts'] = sorted(dataProducts)
        delStatus['complete'] = complete
        delStatus['ousUID'] = ousUID
        retcode,retmsg = dbcon.save(dbName, ousUID, delStatus)
        if retcode != 201:
            raise RuntimeError("setSubstate: error %d, %s" % (retcode,retmsg))
    
    def callback(self, message):
        """
        Message is a JSON object:
            ousUID is the UID of the OUS
             timestamp is the Pipeline run's timestamp
            productsDir is the name of the products directory for that Pipeline run
        
        For instance
            { 
                "ousUID" : "uid://X1/X1/Xc1", 
                "timestamp" : "2018-07-23T09:44:13.604", 
                "productsDir" : "2015.1.00657.S_2018_07_23T09_44_13.604"
            }
        """
    
        print(">>> message:", message)
        ousUID = message["ousUID"]
        timestamp = message["timestamp"]
        productsDir = message["productsDir"]
    
        self.setSubstate(ousUID, 'IngestionTriggered')
    
        # Locate the data products in the replicated cache dir
        dataProductsDir = os.path.join(self.localCache, productsDir, "SOUS", "GOUS", self.encode(ousUID), "products")
        dataProdNames = os.listdir(dataProductsDir)
        time.sleep(5)    # pretend this actually takes time
    
        self.setSubstate(ousUID, 'AnalyzingProducts')
        time.sleep(3)    # pretend this actually takes time
    
        self.setSubstate(ousUID, 'IngestingProducts')
        progID = productsDir.split('_')[0]
        ingestedDataProds = []
        for dataProdName in sorted(dataProdNames):
            if dataProdName.startswith("product") and dataProdName.endswith(".data"):
                dataProdPathname = os.path.join(dataProductsDir, dataProdName)
                print(">>> Ingesting:", dataProdPathname)
                self.ngascon.put(dataProdPathname)
                self.writeMetadata(progID, ousUID, timestamp, dataProdName)
                ingestedDataProds.append(dataProdName)
                self.writeDeliveryStatus(progID, ousUID, timestamp, ingestedDataProds)
                time.sleep(2)    # pretend this actually takes time
    
        # Now populate the ASA metadata and delivery status tables
        self.writeDeliveryStatus(progID, ousUID, timestamp, ingestedDataProds, complete=True)
    
        self.setSubstate(ousUID, 'ProductsIngested')


if __name__ == "__main__":
    # Make sure we know where the local replicated cache directory is
    self.localCache = os.environ.get('DRAWS_LOCAL_CACHE')
    if self.localCache == None:
        raise RuntimeError("DRAWS_LOCAL_CACHE env variable is not defined")
    
    pi = ProductIngestor(self.localCache)
    pi.start()
