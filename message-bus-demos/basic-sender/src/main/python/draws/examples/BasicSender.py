import os
import argparse

from draws.messages.Publisher import Publisher
from draws.messages.rabbitmq.RabbitMqMessageBroker import RabbitMqMessageBroker

from alma.adapt.examples.common.Person import Person

class BasicSender:
    #private Logger logger = LoggerFactory.getLogger( BasicReceiver.class );
    def __init__(self):
        self.__broker = RabbitMqMessageBroker("amqp://localhost:5672", "guest", "guest")
    def run(self, args):
        publisher = Publisher(self.__broker, args.qname)
        freddie = Person("Freddie Mercury", 45, False)
        print("Sending to " + args.qname)
        envelope = publisher.publish(freddie)
        print(">>> Sent to " + args.qname + ": " + str(envelope.getMessage()))
        self.__broker.closeConnection()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--qname", "-q", help="Queue name to be used.", type=str, required=True)
    args = parser.parse_args()
    sender = BasicSender()
    sender.run(args)
