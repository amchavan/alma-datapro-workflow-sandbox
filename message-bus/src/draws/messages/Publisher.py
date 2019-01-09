class Publisher():
    def __init__(self, messageBroker, messageAddress):
        self.__messageBroker = messageBroker
        self.__messageAddress = messageAddress
    def getMessageBroker(self):
        return self.__messageBroker
    def publish(self, message, timeToLive=0):
        self.__messageBroker.send(self.__messageAddress, message, timeToLive)
