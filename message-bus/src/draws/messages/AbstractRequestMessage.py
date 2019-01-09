from draws.messages.Envelope import Envelope
from draws.messages.RequestMessage import RequestMessage
from draws.messages.AbstractMessage import AbstractMessage

class AbstractRequestMessage(AbstractMessage, RequestMessage):
    def __init__(self):
        super(AbstractMessage, self).__init__()
        self.__responseQueueName = None
    def getResponseQueueName(self):
        return self.__responseQueueName
    def setResponseQueueName(self, responseQueueName):
        self.__responseQueueName = responseQueueName

