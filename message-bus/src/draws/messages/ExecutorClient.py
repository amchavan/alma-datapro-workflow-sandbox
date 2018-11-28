from draws.messages.MessageBroker import MessageBroker

class ExecutorClient:
    def __init__(self, queue, consumer ):
        self.__queue = queue
        self.__consumer = consumer
    
    def call(self, request, timeout=0):
        envelope = self.__queue.send(request)
        correlationId = envelope.getId()
        responseQueue = self.__queue.getMessageBroker().messageQueue(correlationId)
        MessageBroker.sleep(100)
        response = responseQueue.receive(timeout)
        responseQueue.delete()
        self.__consumer.consume(response.getMessage())
