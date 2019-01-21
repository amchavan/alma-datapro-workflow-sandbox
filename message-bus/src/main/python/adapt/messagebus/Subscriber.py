import re

class Subscriber(object):
    def __init__(self, messageBroker, queueName, serviceName):
        if messageBroker is None or queueName is None or serviceName is None:
            raise Exception("Null arg")
        if re.match("^[a-zA-Z_][a-zA-Z_0-9]*$", serviceName) is None:
            raise Exception("Invalid serviceName")
        self.__messageBroker = messageBroker
        self.__queue = self.__messageBroker.messageQueue(queueName, serviceName)
    def getQueue(self):
        return self.__queue
    def listen(self, consumer, timeout):
        self.__messageBroker.listen(self.__queue, consumer, timeout)
    def listenInThread(self, consumer, timeout):
        return self.__messageBroker.listenInThread(self.__queue, consumer, timeout)
    def receive(self, timeout=0):
        return self.__messageBroker.receive(self.__queue, timeout)
    def setAcceptedRoles(self, acceptedRoles):
        if self.__messageBroker.getTokenFactory() is None:
            raise Exception("No token factory found")
        self.__queue.setAcceptedRoles(acceptedRoles)
