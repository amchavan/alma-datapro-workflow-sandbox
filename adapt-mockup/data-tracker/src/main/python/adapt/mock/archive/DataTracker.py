#!/usr/bin/env python3

import sys
# import random
import argparse
import os
import time
sys.path.insert(0, "../shared")
from dbmsgq import ExecutorClient
from dbcon import DbConnection
from ngascon import NgasConnection
import dbdrwutils

# Mock-up of the Data Checker
# This implementation does not receive messages 

class DataTracker():
    def __init__(self):
        self.baseUrl = "http://localhost:5984" # CouchDB
        self.xtss = ExecutorClient('localhost', 'msgq', 'xtss')
        self.dbcon = DbConnection(self.baseUrl)
        self.ngas = NgasConnection()

    def start(self):
        # Loop forever
        while True:
            startTime = time.time()
            # see if we find any OUSs with state=DeliveryInProgress, substate=ProductsIngested
            dbName      = 'status-entities'
            selector = {
                "selector": {
                    "$and": [
                        {"state": "DeliveryInProgress"},
                        {"substate": "ProductsIngested"}
                    ]
                },
                "fields": ["entityId", "timestamp"]
            }
            retcode,ousStatuses = self.dbcon.find(dbName, selector)
            if retcode != 200:
                raise RuntimeError("find: %s: error %d: %s" % (dbName, retcode, OUSs))
        
            # For each OUS status entity we found, see if all data was actually replicated here
            if len(ousStatuses) > 0:    
                startTime = time.time()        # Reset startTime for incremental waiting
                ousStatuses = sorted(ousStatuses, key=self.compareByTimestamp)
                for ous in ousStatuses:
                    ousUID = ous['entityId']
                    # ts = ous['timestamp']
                    # print(">>> found", ousUID, ts)
        
                    delStatus = retrieveProductsStatus(ousUID)
                    allReplicated = checkReplication(delStatus)
                    if allReplicated: #This OUS can be delivered
                        deliverOUS(ousUID)
            dbdrwutils.incrementalSleep(startTime)

    def retrieveProductsStatus(self, ousUID):
        # Retrieve the list of products from the delivery status 
        encodedUID = dbdrwutils.encode(ousUID)
        dbName      = 'delivery-status'
        retcode,delStatus = self.dbcon.findOne(dbName, encodedUID)
        if retcode != 200:
            raise RuntimeError("find: %s: error %d: %s" % (dbName, retcode, delStatus))
        return delStatus

    def checkReplication(self, delStatus):
        # See if all those data products were replicated here
        dataProducts = delStatus['dataProducts']
        allReplicated = True
        for dataProduct in dataProducts:
            # print(">>> found", dataProduct)
            if (not self.ngas.check(dataProduct)):
                allReplicated = False
                break
        return allReplicated

    def deliverOUS(self, ousUID):
        dbdrwutils.setState(xtss, ousUID, "Delivered")
        dbdrwutils.setSubstate(xtss, ousUID, "")
        dbdrwutils.clearExecutive(xtss, ousUID)
        time.sleep(5)    # Pretend this actually took some time
        print(">>> OUS", ousUID, "is now Delivered")

    def compareByTimestamp(self, record):
        return record['timestamp']


if __name__ == "__main__":
    dt = DataTracker()
    dt.start()
