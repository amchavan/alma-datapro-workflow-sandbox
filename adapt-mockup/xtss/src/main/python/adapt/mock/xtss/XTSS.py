#!/usr/bin/env python3
import sys
import json
import datetime

from collections import namedtuple

from adapt.messagebus.rabbitmq.Subscriber import Subscriber
from adapt.messagebus.rabbitmq.RabbitMqMessageBroker import RabbitMqMessageBroker

from adapt.mock.messages.gen.XTSSSetState import XTSSSetState

sys.path.insert(0, "../shared")
from dbmsgq import Executor
from dbcon import DbConnection

# Mockup of the XTSS, listens on the xtss queue
class XTSS():
    def __init__(self):
        self._baseUrl = "http://localhost:5984" # CouchDB
        self._dbcon = DbConnection(self._baseUrl)
        self._dbName = "status-entities"
        self._broker = RabbitMqMessageBroker()
        self._subscriber = Subscriber(self._broker, 'xtss.transitions', 'xtss')

    def start(self):
        executor = Executor('localhost', 'msgq', 'xtss', self.xtss)
        print(" [x] Awaiting RPC requests to 'xtss'")
        executor.run()

    def __nowISO(self):
        return datetime.datetime.utcnow().isoformat()[:-3]

    def findOUSStatus(self, ousUID):
        "Find an OUSStatus with the given ID, raise an error if none are found"

        retcode,ousStatus = self._dbcon.findOne(self._dbName, ousUID)
        if retcode == 404:
            raise RuntimeError("OUS not found: %s" % ousUID)
        return ousStatus

    def setField(self, ousUID, fieldName, fieldValue):
        "Set the value of a field of an OUSStatus, update its timestamp"
        ousStatus = self.findOUSStatus(ousUID)
        ousStatus[fieldName] = fieldValue
        ousStatus['timestamp'] = self.__nowISO()
        retcode,msg = self._dbcon.save(self._dbName, ousUID, ousStatus)
        return retcode

    def setState(self, ousUID, state):
        "Set the state of an OUSStatus"
        return self.setField ousUID, 'state', state)

    def setSubstate(self, ousUID, substate):
        "Set the substate of an OUSStatus"
        return self.setField(ousUID, 'substate', substate)

    def setExecutive(self, ousUID, executive):
        "Set the Executive of an OUSStatus"
        return self.setFlag(self, ousUID, 'PL_PROCESSING_EXECUTIVE', executive)

    def clearExecutive(self, ousUID):
        "Clear the Executive of an OUSStatus"
        return self.clearFlag ousUID, 'PL_PROCESSING_EXECUTIVE')

    def setFlag(self, ousUID, name, value):
        "Set an OUSStatus flag"
        ousStatus = self.findOUSStatus ousUID)
        if 'flags' in ousStatus:
            flags = ousStatus['flags']
        else:
            flags = {}
        flags[name] = value
        return self.setField(ousUID, 'flags', flags)

    def clearFlag(self, ousUID, name):
        "Clear an OUSStatus flag"
        ousStatus = self.findOUSStatus(ousUID)
        if 'flags' in ousStatus:
            flags = ousStatus['flags']
        else:
            flags = {}
        if name in flags:
            del flags[name]
        return self.setField(ousUID, 'flags', flags)

    def findByStateSubstate(self, state, substate):
        """
        	Returns a return code and, if all was well and the code is 200, 
        all OUSs with the given state and substate; note substate is
        interpreted as a regexp
        """
        selector = {
           "selector": {
                "state": state,
                "substate": { "$regex": substate }
        }
        retcode,ouss = self._dbcon.find("status-entities", selector)
        if retcode == 200:
            ouss.sort(key=lambda x: x['entityId'])
        return retcode,ouss



    def xtss(self, body):
    """
        Expects the body of the request to be a JSON document including field
        "operation". The name and value of other fields depend on 
        the operation itself. For instance:
            { 
                "operation":"set-state", 
                "ousUID":"uid://A003/X1/X1a", 
                "value":"ReadyForReview"
            }
        or:
            { 
                "operation":"get-ouss-by-state-substate", 
                "state":"ReadyForProcessing", 
                "substate":"^Pipeline"
            }

        For 'set' operations, returns 201 (created) if all was well.
        For 'get' operations, returns .
    """
        print(" [*] request: ", body)
        operation = body["operation"]

        if operation.startswith("set-"):
            ousUID = body["ousUID"]
            value = body["value"]

        if operation == "set-state":
            retcode = self.setState(ousUID=ousUID, state=value)
            return retcode

        elif operation == "set-substate":
            retcode = self.setSubstate(ousUID=ousUID, substate=value)
            return retcode

        elif operation == "set-exec":
            retcode = self.setExecutive(ousUID=ousUID, executive=value)
            return retcode

        elif operation == "clear-exec":
            ousUID = body["ousUID"]
            retcode = self.clearExecutive(ousUID=ousUID)
            return retcode

        elif operation == "get-ouss-by-state-substate":
            # Currently unused
            state = body["state"]
            substate = body["substate"]
            retcode,ouss = self.findByStateSubstate(state, substate)
            if retcode != 200:
                return retcode
            return ouss

        else:
            return "Unsupported operation: " + operation

        return None

if __name__ == "__main__":
    xtss = XTSS()
    xtss.start()
