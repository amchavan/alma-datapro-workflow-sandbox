from draws.messages.MessageQueue import MessageQueue

class RabbitMqMessageQueue(MessageQueue):
    def __init__(queueName, broker, rmqQueueName):
        super(queueName, broker)
        self.__rmqQueueName = rmqQueueName

    def getRmqQueueName(self):
        return self.__rmqQueueName;
