#!/usr/bin/env python3

import os
import sys
import random
import time
import argparse

from adapt.messagebus.rabbitmq.Publisher import Publisher
from adapt.messagebus.rabbitmq.RabbitMqMessageBroker import RabbitMqMessageBroker

sys.path.insert(0, "../shared")
from dbmsgq import MqConnection, ExecutorClient
from dbcon import DbConnection
import dbdrwutils

# Mock-up of the Data Reduction Assistent tool
# Depends on the DRAWS_LOCATION env variable

class DRA():
    def __init__(self, location):
        self._baseUrl = "http://localhost:5984" # CouchDB
        self._dbconn  = DbConnection(baseUrl)
        self._xtss = ExecutorClient('localhost', 'msgq', 'xtss')
        self._mq = MqConnection('localhost', 'msgq')
        self._broker = RabbitMqMessageBroker()
        self.location = location

    def findReadyForPipelineProcessing(self):
        """
        Returns all ReadyForProcessing OUSs with a Pipeline Recipe, if 
            any are found; None otherwise
        """
        selector = {
            "selector": {
                "state": "ReadyForProcessing",
                "substate": {"$regex": "^Pipeline"}
            }
        }
        retcode,ouss = self._dbconn.find("status-entities", selector)
        if len(ouss) == 0:
            return None
        if retcode != 200:
            print(ouss)
            return None
    
        ouss.sort(key=lambda x: x['entityId'])
        return ouss

    def start(self):
        # This is the program's text-based UI
        # Loop forever:
        #   Show Pipeline runs ready for processing
        #    Ask for an OUS UID
        #    Set:
        #       state=Processing
        #       PL_PROCESSING_EXECUTIVE=$DRAWS_LOCATION
        
        while True:
            print()
            print()
            print('------------------------------------------')
            print()

            print("ReadyForProcessing OUSs")
            ouss = self.findReadyForPipelineProcessing()
            if (ouss == None or len(ouss) == 0):
                print("(none)")
                sys.exit()
            else:
                ousMap = {}
                for ous in ouss:
                    entityId = ous['entityId']
                    print(entityId)
                    ousMap[entityId] = ous

            print()
            ousUID = input('Please enter an OUS UID, will be processed at %s: ' % self.location)
            if not (ousUID in ousMap):
                print("No OUS with UID='%s'" % (ousUID))
                continue
            ous = ousMap[ousUID]

            # We are going to process this OUS, set its state and processing executive accordingly
            dbdrwutils.setState(self._xtss, ousUID, "Processing")
            dbdrwutils.setExecutive(self._xtss, ousUID, self.location)

            # Launch the Pipeline Driver on Torque/Maui (pretending it listens to messages)
            message = {}
            message['ousUID'] = ousUID
            message['recipe'] = ous["substate"]
            message['progID'] = ous["progID"]
            # msgTemplate = '{ "ousUID" : "%s", "recipe" : "%s", "progID" : "%s" }'
            # message = msgTemplate % (ousUID, ous["substate"], ous["progID"])
            torque = "pipeline.process." + self.location
            dbdrwutils.sendMsgToSelector(message, torque, self._mq)

            # Wait some, mainly for effect
            waitTime = random.randint(3,8)
            time.sleep(waitTime)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Data Reducer Assignment tool mockup')
    parser.add_argument("--assign-to", "-a",  dest="location",  help="Location where Pipeline jobs should run")
    args=parser.parse_args()
    location = args.location
    # Make sure we know where we should assign Pipeline jobs
    if location == None:
        location = os.environ.get('DRAWS_LOCATION')
        if location == None:
            raise RuntimeError("DRAWS_LOCATION env variable is not defined and -a option not given")
    dra = DRA(location)
