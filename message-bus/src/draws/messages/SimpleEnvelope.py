import uuid
import json
import inspect
import importlib

from draws.messages.Envelope import Envelope
from draws.messages.Envelope import State
from draws.messages.MessageBroker import MessageBroker
from draws.messages.AbstractMessage import AbstractMessage

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
        self._message = message
        if self._new and self._message is not None: self._message.setEnvelope(self)
        self._messageClass = message.__class__.__module__ + "," + message.__class__.__qualname__ if self._new else messageClass
        self._sentTimestamp = sentTimestamp
        self._receivedTimestamp = receivedTimestamp
        self._consumedTimestamp = consumedTimestamp
        self._expiredTimestamp = expiredTimestamp
        self._originIP = originIP
        self._queueName = queueName
        self._state = state
        self._expireTime = expireTime
        self._token = None

    def serialize(self):
        ret = self.__dict__.copy()
        if "_message" in ret and ret["_message"] is not None:
            ret["_message"] = ret["_message"].serialize()
        if "_state" in ret and ret["_state"] is not None:
            ret["_state"] = ret["_state"].name
        return ret

    def deserialize(self, dct):
        self.__dict__ = dct.copy()
        if "_messageClass" in dct and "_message" in dct:
            if dct["_messageClass"] is not None and dct["_message"] is not None:
                cls = SimpleEnvelope._loadClass(dct["_messageClass"].split(",")[0], dct["_messageClass"].split(",")[1], AbstractMessage)
                message = cls()
                message.deserialize(dct["_message"])
                message.setEnvelope(self)
                self.__dict__["_message"] = message
        if "_state" in dct and dct["_state"] is not None:
            self.__dict__["_state"] = State[dct["_state"]]

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
        if (self._consumedTimestamp is None) and (obj._consumedTimestamp is not None):
            return False
        if (self._consumedTimestamp is not None) and (not self._consumedTimestamp.equals(obj._consumedTimestamp)):
            return False
        if (self._expireTime != obj._expireTime):
            return False
        if (self._expiredTimestamp is None) and (obj._expiredTimestamp is not None):
            return False
        if (self._expiredTimestamp is not None) and (not self._expiredTimestamp.equals(obj._expiredTimestamp)):
            return False
        if (self._id is None) and (other.id is None):
            return False
        if (self._id is not None) and self._id != obj._id:
            return False
        if (self._message is None) and (obj.message is not None):
            return False
        if (self._message is not None) and not self._message.equals(obj._message):
            return False
        if (self._messageClass is None) and (obj._messageClass is not None):
            return False
        if (self._messageClass is not None) and (self._messageClass != obj._messageClass):
            return False
        if (self._originIP is None) and (obj._originIP is not None):
            return False
        if (self._originIP is not None) and self._originIP != obj._originIP:
            return False
        if (self._queueName is None) and (obj._queueName is not None):
            return False
        if (self._queueName is not None) and self._queueName != obj._queueName:
            return False
        if (self._receivedTimestamp is None) and (obj._receivedTimestamp is not None):
            return False
        if (self._receivedTimestamp is not None) and (not self._receivedTimestamp.equals(obj._receivedTimestamp)):
            return False
        if (self._sentTimestamp is None) and (obj._sentTimestamp is not None):
            return False
        if (self._sentTimestamp is not None) and (not self._sentTimestamp.equals(obj._sentTimestamp)):
            return False
        if (self._state != obj._state):
            return False
        return True

    def getConsumedTimestamp(self):
        return self._consumedTimestamp
    
    def getExpiredTimestamp(self):
        return self._expiredTimestamp
    
    def getExpireTime(self):
        return self._expireTime
    
    def getId(self):
        return self._id
    
    def getMessage(self):
        return self._message
    
    def getMessageClass(self):
        return self._messageClass
    
    def getOriginIP(self):
        return self._originIP
    
    def getQueueName(self):
        return self._queueName
    
    def getReceivedTimestamp(self):
        return self._receivedTimestamp
    
    def getRejectedTimestamp(self):
        return self._rejectedTimestamp
    
    def getSentTimestamp(self):
        return self._sentTimestamp
    
    def getState(self):
        return self._state

    def getTimeToLive(self):
        # Was this message read at some point in the past?
        # But wait: does it even expire at all?
        if((self._expireTime == 0) or (self.getState() != State.Sent)):
            # YES, not expiring
            return -1
        # Should never happen, except maybe in tests
        if(self._sentTimestamp is None):
            return -1
        try:
            sent = parseIsoDatetime(sentTimestamp)
            now = now()
            timeLived = now.getTime() - sent.getTime()
            remainingTimeToLive = self._expireTime - timeLived
            
            print( ">>> Envelope: " + self )
            print( ">>>     now: " + now )
            print( ">>>     timeToLive: " + self._expireTime )
            print( ">>>     timeLived: " + timeLived )
            print( ">>>     remainingTimeToLive: " + remainingTimeToLive )
            return 0 if (remainingTimeToLive <= 0) else remainingTimeToLive
        except ParseException as e:
            # TODO Improve logging of this
            e.printStackTrace()
            raise RuntimeException(e)

    def getToken(self):
        return self._token
    def hashCode(self):
        prime = 31
        result = 1
        result = prime * result + (0 if (self._consumedTimestamp is None) else consumedTimestamp.hashCode())
        result = prime * result + int(self._expireTime ^ (self._expireTime >> 32))
        result = prime * result + (0 if (self._expiredTimestamp is None) else self._expiredTimestamp.hashCode())
        result = prime * result + (0 if (self._id is None) else self._id.hashCode())
        result = prime * result + (0 if (self._message is None) else self._message.hashCode())
        result = prime * result + (0 if (self._messageClass is None) else self._messageClass.hashCode())
        result = prime * result + (0 if (self._originIP is None) else self._originIP.hashCode())
        result = prime * result + (0 if (self._queueName is None) else self._queueName.hashCode())
        result = prime * result + (0 if (self._receivedTimestamp is None) else self._receivedTimestamp.hashCode())
        result = prime * result + (0 if (self._sentTimestamp is None) else self._sentTimestamp.hashCode())
        result = prime * result + (0 if (self._state is None) else self._state.hashCode())
        return result
    def setConsumedTimestamp(self, consumedTimestamp):
        self._consumedTimestamp = consumedTimestamp
    def setExpiredTimestamp(self, expiredTimestamp):
        self._expiredTimestamp = expiredTimestamp
    def setExpireTime(self, expireTime):
        self._expireTime = expireTime
    def setId(self, _id):
        self._id = _id
    def setMessage(self, message):
        self._message = message
    def setMessageClass(self, messageClass):
        self._messageClass = messageClass
    def setOriginIP(self, originIP):
        self._originIP = originIP
    def setQueueName(self, queueName):
        self._queueName = queueName
    def setReceivedTimestamp(self, receivedTimestamp):
        self._receivedTimestamp = receivedTimestamp
    def setRejectedTimestamp(self, rejectedTimestamp):
        self._rejectedTimestamp = rejectedTimestamp
    def setSentTimestamp(self, sentTimestamp):
        self._sentTimestamp = sentTimestamp
    def setState(self, state):
        self._state = state
    def setToken(self, token):
        self._token = token
    def __str__(self):
        msg = self.__class__.__qualname__ + "[message=" + str(self._message) + ", sent=" + self._sentTimestamp + ", originIP=" + self._originIP + ", queueName=" + self._queueName + ", state=" + self._state.name
        if self._token is not None:
            msg += ", token=" + self._token[0:10] + "..."
        msg += "]"
        return msg
