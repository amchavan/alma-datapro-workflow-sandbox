import os
import argparse

from adapt.messagebus.Subscriber import Subscriber
from adapt.messagebus.rabbitmq.RabbitMqMessageBroker import RabbitMqMessageBroker

class BasicReceiver(object):
    def __init__(self):
        self.__broker = RabbitMqMessageBroker("amqp://localhost:5672", "guest", "guest")
    def run(self, args):
        subscriber = Subscriber(self.__broker, args.qname, args.sname)
        print("Waiting for message")
        received = subscriber.receive(120*1000)
        msg = args.sname + "(" + str(os.getpid()) + ") received: " + str(received.getMessage())
        self.__broker.closeConnection()
        print(msg)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--qname", "-q", help="Queue name to be used.", type=str, required=True)
    parser.add_argument("--sname", "-s", help="Service name to be used.", type=str, required=True)
    args = parser.parse_args()
    receiver = BasicReceiver()
    receiver.run(args)
    

