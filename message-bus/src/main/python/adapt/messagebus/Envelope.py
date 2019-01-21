from enum import Enum

State = Enum('State', 'Sent Received Consumed Expired Rejected')
class Envelope(object):
    def getConsumedTimestamp(self):
        raise NotImplementedError
    def getExpiredTimestamp(self):
        raise NotImplementedError
    def getId(self):
        raise NotImplementedError
    def getMessage(self):
        raise NotImplementedError
    def getMessageClass(self):
        raise NotImplementedError
    def getOriginIP(self):
        raise NotImplementedError
    def getQueueName(self):
        raise NotImplementedError
    def getReceivedTimestamp(self):
        raise NotImplementedError
    def getSentTimestamp(self):
        raise NotImplementedError
    def getRejectedTimestamp(self):
        raise NotImplementedError
    def getState(self):
        raise NotImplementedError
    def getTimeToLive(self):
        raise NotImplementedError
    def getToken(self):
        raise NotImplementedError
    def setMessage(self, message):
        raise NotImplementedError
