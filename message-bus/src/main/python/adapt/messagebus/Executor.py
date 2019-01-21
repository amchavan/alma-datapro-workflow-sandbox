from adapt.messagebus.MessageConsumer import MessageConsumer
from adapt.messagebus.Publisher import Publisher

class Executor(object):
    class ExecutorConsumer(MessageConsumer):
        def __init__(self, subscriber, processor):
            self.__subscriber = subscriber
            self.__processor = processor
        def consume(self, message):
            response = self.__processor.process(message)
            broker = self.__subscriber.getQueue().getMessageBroker()
            publisher = Publisher(broker, message.getResponseQueueName())
            publisher.publish(response, 0)
    def __init__(self, subscriber, processor, timeout):
        self.__subscriber = subscriber
        self.__processor = processor
        self.__timeout = timeout
        self.__consumer = Executor.ExecutorConsumer(subscriber, processor)
    def run(self):
        self.__subscriber.listen(self.__consumer, self.__timeout)
