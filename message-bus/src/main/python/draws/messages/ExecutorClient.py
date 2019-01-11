import uuid

from draws.messages.MessageBroker import MessageBroker
from draws.messages.Subscriber import Subscriber
from draws.messages.Publisher import Publisher

class ExecutorClient:
    #Static Methods
    @classmethod
    def makeResponseQueueName(cls):
        _id = "response-queue-"
        _id += MessageBroker.nowISO()
        _id += "-"
        _id += str(uuid.uuid4()).replace("-", "")
        return _id

    #Instance Methods
    def __init__(self, publisher, consumer ):
        self.__publisher = publisher
        self.__consumer = consumer
    def call(self, request, timeout=0):
        responseQueueName = ExecutorClient.makeResponseQueueName()
        subscriber = Subscriber(self.__publisher.getMessageBroker(), responseQueueName, "temp")
        request.setResponseQueueName(responseQueueName)
        self.__publisher.publish(request)
        response = subscriber.receive(timeout)
        subscriber.getQueue().delete()
        self.__consumer.consume(response.getMessage())
