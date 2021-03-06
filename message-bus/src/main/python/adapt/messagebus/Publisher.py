class Publisher(object):
    def __init__(self, messageBroker, messageAddress):
        self.__messageBroker = messageBroker
        self.__messageAddress = messageAddress
    def getMessageBroker(self):
        return self.__messageBroker
    def publish(self, message, timeToLive=0):
        return self.__messageBroker.send(self.__messageAddress, message, timeToLive)
