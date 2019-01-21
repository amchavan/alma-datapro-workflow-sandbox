from adapt.messagebus.Message import Message

class RequestMessage(Message):
    def getResponseQueueName():
        raise NotImplementedError
    def setResponseQueueName():
        raise NotImplementedError
