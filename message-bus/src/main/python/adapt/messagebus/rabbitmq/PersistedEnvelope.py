import json

from adapt.messagebus.SimpleEnvelope import SimpleEnvelope

class PersistedEnvelope:
    @classmethod
    def convert(cls, envelope):
        ret = PersistedEnvelope()
        ret._envelopeId         = envelope.getId()
        ret._consumedTimestamp  = envelope.getConsumedTimestamp()
        ret._expiredTimestamp   = envelope.getExpiredTimestamp()
        ret.messageClass       = envelope.getMessageClass()
        ret._originIp           = envelope.getOriginIP()
        ret._queueName          = envelope.getQueueName()
        ret._receivedTimestamp  = envelope.getReceivedTimestamp()
        ret._sentTimestamp      = envelope.getSentTimestamp()
        ret._state              = envelope.getState().name
        ret._timeToLive         = envelope.getTimeToLive()
        try:
            m = envelope.getMessage()
            jsons = json.dumps(m, default=SimpleEnvelope.serialize)
            ret.message = jsons
        except Exception as e:
            raise Exception(e)
        return ret

    def __init__(self):
        self._id = None
        self._envelopeId = None
        self._consumedTimestamp = None
        self._expiredTimestamp = None
        self.message = None
        self.messageClass = None
        self._originIp = None 
        self._queueName = None
        self._receivedTimestamp = None
        self._sentTimestamp = None
        self._rejectedTimestamp = None
        self._state = None
        self._timeToLive = None
    
    def asSimpleEnvelope(self):
        deserializedMessage = SimpleEnvelope.deserializeMessage(self.messageClass, self.message)
        ret = SimpleEnvelope(envelopeId, deserializedMessage, messageClass, sentTimestamp, receivedTimestamp, consumedTimestamp, expiredTimestamp, originIp, queueName, State.valueOf( state ), timeToLive )
        return ret

    def __str__(self):
        return this.__class__.__qualname__ + "[" + self._envelopeId + "]"
