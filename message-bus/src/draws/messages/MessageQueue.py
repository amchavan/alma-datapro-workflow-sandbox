from enum import Enum

Type = Enum('State', 'SEND RECEIVE SENDQUEUE')
class MessageQueue:
    def __str__(self):
        return self.__class__.__qualname__ + "[" + self.__queueName + "]"
    def __init__(self, queueName, messageBus, mqtype=Type.RECEIVE):
        if queueName is None or messageBus is None:
            raise IllegalArgumentException("Null arg")
        self.__queueName = queueName
        self.__messageBroker = messageBus
        self.__acceptedRoles = None
        self.__type = mqtype
    def delete(self):
        self.__messageBroker.deleteQueue(self)
    def getAcceptedRoles(self):
        return self.__acceptedRoles
    def getMessageBroker(self):
        return self.__messageBroker
    def getName(self):
        return self.__queueName
    def getType(self):
        return self.__type
    def joinGroup(self, groupName):
        self.__messageBroker.joinGroup(self.__queueName, groupName)
    def setAcceptedRoles(self, acceptedRoles):
        if self.__messageBroker.getTokenFactory() is None:
            raise Exception("No token factory found")
        this.acceptedRoles = acceptedRoles
