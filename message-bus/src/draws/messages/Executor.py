from draws.messages.MessageConsumer import MessageConsumer

class Executor:
    class ExecutorConsumer(MessageConsumer):
        def __init__(self, queue, processor):
            self.__queue = queue
            self.__processor = processor
        def consume(self, message):
            response = self.__processor.process(message)
            envelope = message.getEnvelope()
            responseQueue = self.__queue.getMessageBroker().messageQueue(envelope.getId())
            responseQueue.send(response, 0)
    def __init__(self, queue, processor, timeout):
        self.__queue = queue
        self.__processor = processor
        self.__timeout = timeout
        self.__consumer = Executor.ExecutorConsumer(queue, processor)
    def run(self):
        self.__queue.listen(self.__consumer, self.__timeout)
