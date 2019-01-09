class Subscriber():
    def __init__(self, messageBroker, queueName, serviceName):
        self.__messageBroker = messageBroker
        self.__messageBroker.setServiceName(serviceName)
        self.__queue = self.__messageBroker.messageQueue(queueName)
    def getQueue(self):
        return self.__queue
    def listen(self, consumer, timeout):
        self.__messageBroker.listen(self.__queue, consumer, timeout)
    def listenInThread(self, consumer, timeout):
        return self.__messageBroker.listenInThread(self.__queue, consumer, timeout)
    def receive(self, timeout=0):
        return self.__messageBroker.receive(self.__queue, timeout)
