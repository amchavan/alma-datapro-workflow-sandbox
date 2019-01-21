import uuid
import json
import inspect
import importlib

from adapt.messagebus.Envelope import Envelope
from adapt.messagebus.Envelope import State
from adapt.messagebus.MessageBroker import MessageBroker
from adapt.messagebus.AbstractMessage import AbstractMessage

class SimpleEnvelope(Envelope):
    @classmethod
    def _loadClass(cls, mod, cl, inh=None):
        c = None
        try:
            m = importlib.import_module(mod)
        except ModuleNotFoundError as e:
            raise Exception("Module '" + mod +"' not found.", e)
        try:
            clels = cl.split(".")
            mel = m
            for clel in clels:
                c = inspect.getattr_static(mel, clel)
                if not clel == clels[-1]:
                    mel = c
            if not inspect.isclass(c):
                raise Exception("Class '" + cl +"' not found in module '" + mod + "'.")
            if inh is not None and not issubclass(c, inh):
                raise Exception("Class '" + cl +"' is not an instance of '" + inh.__qualname__ + "' class.")
            c = getattr(mel, clels[-1])
        except AttributeError as e:
            raise Exception("Class '" + cl + "' not found in module '" + mod + "'.", e)
        if c is None:
            raise Exception("Class '" + cl + "' failed to be loaded in an unexpected way.")
        return c

    @classmethod
    def _makeID(cls):
        _id = MessageBroker.nowISO()
        _id += "-"
        _id += str(uuid.uuid4()).replace("-", "")
        return _id

    def __init__(self, _id=None, message=None, messageClass=None, sentTimestamp=None, receivedTimestamp=None, consumedTimestamp=None, expiredTimestamp=None, originIP=None, queueName=None, state=None, expireTime=0):
        super()
        self._new = False
        if _id is None: self._new = True
        self._id = SimpleEnvelope._makeID() if self._new else _id
        self.message = message
        if self._new and self.message is not None: self.message.setEnvelope(self)
        #self.messageClass = message.__class__.__module__ + "," + message.__class__.__qualname__ if self._new else messageClass
        self.messageClass = message.__class__.__module__ if self._new else messageClass
        self.sentTimestamp = sentTimestamp
        self.receivedTimestamp = receivedTimestamp
        self.consumedTimestamp = consumedTimestamp
        self.expiredTimestamp = expiredTimestamp
        self.rejectedTimestamp = None
        self.originIP = originIP
        self.queueName = queueName
        self.state = state
        self.expireTime = expireTime
        self.token = None

    def serialize(self):
        ret = self.__dict__.copy()
        if "message" in ret and ret["message"] is not None:
            ret["message"] = ret["message"].serialize()
        #ret["messageClass"] = "alma.obops.adapt.examples.common.Person"
        if "state" in ret and ret["state"] is not None:
            ret["state"] = ret["state"].name
        if "_new" in ret:
            del ret["_new"]
        return ret

    def deserialize(self, dct):
        self.__dict__ = dct.copy()
        if "messageClass" in dct and "message" in dct:
            if dct["messageClass"] is not None and dct["message"] is not None:
                #cls = SimpleEnvelope._loadClass(dct["messageClass"].split(",")[0], dct["messageClass"].split(",")[1], AbstractMessage)
                cls = SimpleEnvelope._loadClass(dct["messageClass"],dct["messageClass"].rsplit(".",1)[1])
                message = cls()
                message.deserialize(dct["message"])
                message.setEnvelope(self)
                self.__dict__["message"] = message
        if "state" in dct and dct["state"] is not None:
            self.__dict__["state"] = State[dct["state"]]

    def compareTo(self, other):
        return self.getSentTimestamp().compareTo(other.getSentTimestamp())
    
    def __eq__(self, obj):
        return self.equals(obj)

    def equals(self, obj):
        if (self is obj):
            return True
        if (obj is None):
            return False
        if (self.__class__ != obj.__class__):
            return False
        if (self.consumedTimestamp is None) and (obj.consumedTimestamp is not None):
            return False
        if (self.consumedTimestamp is not None) and (not self.consumedTimestamp.equals(obj.consumedTimestamp)):
            return False
        if (self.expireTime != obj.expireTime):
            return False
        if (self.expiredTimestamp is None) and (obj.expiredTimestamp is not None):
            return False
        if (self.expiredTimestamp is not None) and (not self.expiredTimestamp.equals(obj.expiredTimestamp)):
            return False
        if (self._id is None) and (obj._id is None):
            return False
        if (self._id is not None) and self._id != obj._id:
            return False
        if (self.message is None) and (obj.message is not None):
            return False
        if (self.message is not None) and not self.message.equals(obj.message):
            return False
        if (self.messageClass is None) and (obj.messageClass is not None):
            return False
        if (self.messageClass is not None) and (self.messageClass != obj.messageClass):
            return False
        if (self.originIP is None) and (obj.originIP is not None):
            return False
        if (self.originIP is not None) and self.originIP != obj.originIP:
            return False
        if (self.queueName is None) and (obj.queueName is not None):
            return False
        if (self.queueName is not None) and self.queueName != obj.queueName:
            return False
        if (self.receivedTimestamp is None) and (obj.receivedTimestamp is not None):
            return False
        if (self.receivedTimestamp is not None) and (not self.receivedTimestamp.equals(obj.receivedTimestamp)):
            return False
        if (self.sentTimestamp is None) and (obj.sentTimestamp is not None):
            return False
        if (self.sentTimestamp is not None) and (not self.sentTimestamp.equals(obj.sentTimestamp)):
            return False
        if (self.state != obj.state):
            return False
        return True

    def getConsumedTimestamp(self):
        return self.consumedTimestamp
    
    def getExpiredTimestamp(self):
        return self.expiredTimestamp
    
    def getExpireTime(self):
        return self.expireTime
    
    def getId(self):
        return self._id
    
    def getMessage(self):
        return self.message
    
    def getMessageClass(self):
        return self.messageClass
    
    def getOriginIP(self):
        return self.originIP
    
    def getQueueName(self):
        return self.queueName
    
    def getReceivedTimestamp(self):
        return self.receivedTimestamp
    
    def getRejectedTimestamp(self):
        return self.rejectedTimestamp
    
    def getSentTimestamp(self):
        return self.sentTimestamp
    
    def getState(self):
        return self.state

    def getTimeToLive(self):
        # Was this message read at some point in the past?
        # But wait: does it even expire at all?
        if((self.expireTime == 0) or (self.getState() != State.Sent)):
            # YES, not expiring
            return -1
        # Should never happen, except maybe in tests
        if(self.sentTimestamp is None):
            return -1
        try:
            sent = parseIsoDatetime(sentTimestamp)
            now = now()
            timeLived = now.getTime() - sent.getTime()
            remainingTimeToLive = self.expireTime - timeLived
            return 0 if (remainingTimeToLive <= 0) else remainingTimeToLive
        except ParseException as e:
            # TODO Improve logging of this
            e.printStackTrace()
            raise RuntimeException(e)

    def getToken(self):
        return self.token
    def hashCode(self):
        prime = 31
        result = 1
        result = prime * result + (0 if (self.consumedTimestamp is None) else consumedTimestamp.hashCode())
        result = prime * result + int(self.expireTime ^ (self.expireTime >> 32))
        result = prime * result + (0 if (self.expiredTimestamp is None) else self.expiredTimestamp.hashCode())
        result = prime * result + (0 if (self._id is None) else self._id.hashCode())
        result = prime * result + (0 if (self.message is None) else self.message.hashCode())
        result = prime * result + (0 if (self.messageClass is None) else self.messageClass.hashCode())
        result = prime * result + (0 if (self.originIP is None) else self.originIP.hashCode())
        result = prime * result + (0 if (self.queueName is None) else self.queueName.hashCode())
        result = prime * result + (0 if (self.receivedTimestamp is None) else self.receivedTimestamp.hashCode())
        result = prime * result + (0 if (self.sentTimestamp is None) else self.sentTimestamp.hashCode())
        result = prime * result + (0 if (self.state is None) else self.state.hashCode())
        return result
    def setConsumedTimestamp(self, consumedTimestamp):
        self.consumedTimestamp = consumedTimestamp
    def setExpiredTimestamp(self, expiredTimestamp):
        self.expiredTimestamp = expiredTimestamp
    def setExpireTime(self, expireTime):
        self.expireTime = expireTime
    def setId(self, _id):
        self._id = _id
    def setMessage(self, message):
        self.message = message
    def setMessageClass(self, messageClass):
        self.messageClass = messageClass
    def setOriginIP(self, originIP):
        self.originIP = originIP
    def setQueueName(self, queueName):
        self.queueName = queueName
    def setReceivedTimestamp(self, receivedTimestamp):
        self.receivedTimestamp = receivedTimestamp
    def setRejectedTimestamp(self, rejectedTimestamp):
        self.rejectedTimestamp = rejectedTimestamp
    def setSentTimestamp(self, sentTimestamp):
        self.sentTimestamp = sentTimestamp
    def setState(self, state):
        self.state = state
    def setToken(self, token):
        self.token = token
    def __str__(self):
        msg = self.__class__.__qualname__ + "[message=" + str(self.message) + ", sent=" + self.sentTimestamp + ", originIP=" + self.originIP + ", queueName=" + self.queueName + ", state=" + self.state.name
        if self.token is not None:
            msg += ", token=" + self.token[0:10] + "..."
        msg += "]"
        return msg
