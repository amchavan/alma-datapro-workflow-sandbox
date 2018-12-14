class MessageQueue:
    def __init__(self, queueName, messageBus):
        if queueName is None or messageBus is None:
            raise IllegalArgumentException("Null arg")
        self.__queueName = queueName
        self.__messageBroker = messageBus
    def getMessageBroker(self):
        return self.__messageBroker;
    def getName(self):
        return self.__queueName;
    def send(self, message, timeToLive=0):
        return self.__messageBroker.send(self, message, timeToLive)
    def delete(self):
        self.__messageBroker.deleteQueue(self)
    def receive(self, timeout=0):
        return self.__messageBroker.receive(self, timeout)
    def joinGroup(self, groupName):
        self.__messageBroker.joinGroup(self.__queueName, groupName)
    def listen(self, consumer, timeout):
        self.__messageBroker.listen(self, consumer, timeout);
    def listenInThread(self, consumer, timeout):
        return self.__messageBroker.listenInThread(self, consumer, timeout)
    def __str__(self):
        return self.__class__.__qualname__ + "[" + self.__queueName + "]"
