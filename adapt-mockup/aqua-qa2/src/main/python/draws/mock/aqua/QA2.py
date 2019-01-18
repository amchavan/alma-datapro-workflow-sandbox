#!/usr/bin/env python3

import base64
import sys
import random
import time
sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
from dbcon import DbConnection
import dbdrwutils

# Mock-up of AQUA
# TODO

class QA2():
    def __init__(self):
        self.weblogsBaseUrl = "http://localhost:8000"
        self.baseUrl = "http://localhost:5984" # CouchDB
        self.dbconn  = DbConnection(self.baseUrl)
        self.dbName  = "pipeline-reports"
        self.xtss   = ExecutorClient('localhost', 'msgq', 'xtss')
        self.select = "pipeline.report.JAO"
        self.mq     = MqConnection('localhost', 'msgq',  select)
        
    def start(self):
        # Launch the listener in the background
        print(' [*] Waiting for messages matching %s' % (self.select))
        dbdrwutils.bgRun(self.mq.listen, (self.callback,))
        # This is the program's text-based UI
        # Loop forever:
        #   Show Pipeline runs awaiting review
        #    Ask for an OUS UID
        #   Lookup the most recent PL execution for that
        #    Print it out
        #   Ask for Fail, Pass, or SemiPass
        #    Set the OUS state accordingly
        while True:
            print()
            print()
            print('------------------------------------------')
            print()
            print("OUSs ready to be reviewed")
            ouss = self.findReadyForReview()
            if (ouss == None or len(ouss) == 0):
                print("(none)")
            else:
                for ous in ouss:
                    print(ous['entityId'])
            print()
            ousUID = input('Please enter an OUS UID: ')
            plReport = self.findMostRecentPlReport(ousUID)
            if plReport == None:
                print("No Pipeline executions for OUS", ousUID)
                continue
            # We are reviewing this OUS, set its state accordingly
            dbdrwutils.setState(self.xtss, ousUID, "Reviewing")
        
            timestamp = plReport['timestamp']
            report = dbdrwutils.b64decode(plReport['encodedReport'])
            productsDir = plReport['productsDir']
            source = plReport['source']
            print("Pipeline report for UID %s, processed %s" % (ousUID,timestamp))
            print(report)
            print()
            print("Weblog available at: %s/weblogs/%s" % (self.weblogsBaseUrl, dbdrwutils.makeWeblogName(ousUID, timestamp)))
            print()
            while True:
                reply = input("Enter [F]ail, [P]ass, [S]emipass, [C]ancel: ")
                reply = reply[0:1].upper()
                if ((reply=='F') or (reply=='P') or (reply=='S') or (reply=='C')):
                    break
            if reply == 'C':
                continue
            # Set the OUS state according to the QA2 flag
            self.processQA2flag(ousUID, reply)

            if reply == 'F':
                continue
            # Tell the Product Ingestor that it should ingest those Pipeline products
            selector = "ingest.JAO"
            message = '{"ousUID" : "%s", "timestamp" : "%s", "productsDir" : "%s"}' % \
                (ousUID, timestamp, productsDir)
            message = {}
            message["ousUID"]      = ousUID
            message["timestamp"]   = timestamp
            message["productsDir"] = productsDir
            self.mq.send(message, selector)
            # Wait some, mainly for effect
            waitTime = random.randint(3,8)
            time.sleep(waitTime)
            # Now we can set the state of the OUS to DeliveryInProgress
            dbdrwutils.setState(self.xtss, ousUID, "DeliveryInProgress")

    def savePlReport(self, ousUID, timestamp, encodedReport, productsDir, source):
        '''
            Saves a pipeline run report to 'Oracle'
        '''
        plReport = {}
        plReport['ousUID'] = ousUID
        plReport['timestamp'] = timestamp
        plReport['encodedReport'] = encodedReport
        plReport['productsDir'] = productsDir
        plReport['source'] = source
        plReportID = timestamp + "." + ousUID

        retcode,msg = self.dbconn.save(self.dbName, plReportID, plReport)
        if retcode != 201:
            raise RuntimeError("Error saving Pipeline report: %d, %s" % (retcode,msg))

    def findMostRecentPlReport(self, ousUID):
        selector = { "selector": { "ousUID": ousUID }}
        retcode,reports = self.dbconn.find(self.dbName, selector)
        if len(reports) == 0:
            return None
        if retcode != 200:
            print(reports)
            return None
    
        # Find the most recent report and return it
        reports.sort(key=lambda x: x['timestamp'], reverse=True)
        return reports[0]

    def findReadyForReview(self):
        selector = {
           "selector": {
              "state": "ReadyForReview"
           }
        }
        retcode,ouss = self.dbconn.find("status-entities", selector)
        if len(ouss) == 0:
            return None
        if retcode != 200:
            print(ouss)
            return None
    
        ouss.sort(key=lambda x: x['entityId'])
        return ouss

    def processQA2flag(self, ousUID, flag):
        "Flag should be one of 'F' (fail), 'P' (pass) or 'S' (semi-pass)"
        newState = "ReadyForProcessing" if (flag == "F") else "Verified"
        print(">>> Setting the state of", ousUID, "to", newState)
        # Set the OUS state according to the input flag
        dbdrwutils.setState(self.xtss, ousUID, newState)
        if flag == "F":
            dbdrwutils.setSubstate(self.xtss, ousUID, "")    # Clear Pipeline recipe

    def callback(self, message):
        """
        Message is a JSON object:
            ousUID is the UID of the OUS
             source is the executive where the Pipeline was running 
             report is the report's XML text, BASE64-encoded
             timestamp is the Pipeline run's timestamp
            productsDir is the name of the products directory for that Pipeline run
        
        For instance
            {
                "ousUID" : "uid://X1/X1/Xaf", 
                "source" : "EU", 
                  "report" : "Cjw/eG1sIHZlcnNpb2..."
                 "timestamp" : "2018-07-19T08:50:10.228", 
                "productsDir": "2015.1.00657.S_2018_07_19T08_50_10.228"
            }
        """
        # print(">>> message:", message)
        ousUID        = message["ousUID"]
        source        = message["source"]
        encodedReport = message["report"]
        timestamp     = message["timestamp"]
        productsDir   = message["productsDir"]
        # report = dbdrwutils.b64decode(encodedReport)
        # print(">>> report:", report)
        
        # Save the report to Oracle
        self.savePlReport(ousUID, timestamp, encodedReport, productsDir, source)
        print(">>> AQUA/QA2: saved PL report: ousUID=%s, timestamp=%s" % (ousUID,timestamp))

if __name__ == "__main__":
    qa2 = QA2()
    qa2.start()
