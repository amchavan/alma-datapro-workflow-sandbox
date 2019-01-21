import os
import re
import json
import pika
import threading

from draws.messages.Envelope import State
from draws.messages.MessageQueue import MessageQueue
from draws.messages.MessageBroker import MessageBroker
from draws.messages.SimpleEnvelope import SimpleEnvelope
from draws.messages.AbstractMessageBroker import AbstractMessageBroker
from draws.messages.TimeLimitExceededException import TimeLimitExceededException

from draws.messages.rabbitmq.RecipientGroup import RecipientGroup
from draws.messages.rabbitmq.MessageArchiver import MessageArchiver

from draws.messages.configuration.PersistedEnvelopeRepository import PersistedEnvelopeRepository
from draws.messages.configuration.RecipientGroupRepository import RecipientGroupRepository

class RabbitMqMessageBroker(AbstractMessageBroker):
    #Static Variables
    __WAIT_BETWEEN_POLLING_FOR_GET = 500
    MINIMAL_URI = "amqp://@"
    MESSAGE_PERSISTENCE_QUEUE = "message.persistence.queue"
    MESSAGE_STATE_ROUTING_KEY = "new.message.state"
    #Static Methods
    @classmethod
    def makeQueueName(cls, serviceName, queueName):
        return serviceName + "." + queueName
        #return queueName
    class RabbitMqListener:
        def __init__(self, channel, rmqmb, consumer, queue, autoAck=False):
            self.__channel = channel
            self.__rmqmb = rmqmb
            self.__consumer = consumer
            self._consumed = False
            self.__queue = queue
            self._autoAck = autoAck
        def isConsumed(self):
            return self._consumed
        def handleDelivery(self, channel, envelope, properties, body):
            lastDeliveryTime = MessageBroker.now()
            receivedEnvelope = SimpleEnvelope()
            if body is not None:
                dct = json.loads(str(body, "UTF-8"))
                receivedEnvelope.deserialize(dct)
            if not self._autoAck:
                self.__channel.basic_ack(envelope.delivery_tag, False)
            self.__rmqmb._computeState(self.__queue, receivedEnvelope)
            if receivedEnvelope.getState() == State.Received:    
                self.__consumer.consume(receivedEnvelope.getMessage())
                self.__rmqmb._setState(receivedEnvelope, State.Consumed)
                self._consumed = True
            #print("handleDelivery(listen): "+ str(body))

    #Instance Methods
    def __parsePropsFile(self, path):
        propsFile = open(path, 'r')
        lines = propsFile.readlines()
        propsFile.close()
        props = {}
        for l in lines:
            tmp = l.replace('\n', '')
            tmp = tmp.replace("#.*", '')
            tmp = tmp.strip()
            if tmp == '':
                continue
            if '=' not in tmp:
                print("[Warning] Skipping line '" + tmp + "'.")
                continue
            tmp = tmp.split('=')
            props[tmp[0]] = tmp[1]
        return props
    def __initFromProperties(self, baseURI, username, password, envelopeRepository, groupRepository):
        props = {}
        if "ACSDATA" not in os.environ:
            print("[Warning] ACSDATA environment variable not defined")
        else:
            path = os.environ["ACSDATA"] + '/config/archiveConfig.properties'
            if not os.path.exists(path) or not os.path.isfile(path):
                print("[Warning] File '$ACSDATA/config/archiveConfig.properties' doesn't exist or it's not a regular file.")
            else:
                props = self.__parsePropsFile(path)
        if baseURI is not None:
            props['archive.rabbitmq.connection'] = baseURI
        if username is not None:
            props['archive.rabbitmq.username'] = username
        if password is not None:
            props['archive.rabbitmq.password'] = password
        return props

    def __init__(self, baseURI=None, username=None, password=None, exchangeName=MessageBroker.DEFAULT_MESSAGE_BROKER_NAME, envelopeRepository=None, groupRepository=None):
        super(RabbitMqMessageBroker, self).__init__()
        props = self.__initFromProperties(baseURI, username, password, envelopeRepository, groupRepository)
        if 'archive.rabbitmq.connection' not in props:
            raise Exception("Connection argument cannot be null. Either provide a valide '$ACSDATA/config/archiveConfig.properties' file or pass the baseURI argument.")
        parameters = pika.ConnectionParameters()
        try:
            conn = props['archive.rabbitmq.connection'].split(':')
            parameters.host = conn[1].replace('//','')
            parameters.port = conn[2]
        except URISyntaxException | NoSuchAlgorithmException | KeyManagementException as e:
            raise Exception( e )
        if not username is None and not password is None:
            credentials = pika.PlainCredentials(username, password)
            parameters.credentials = credentials
        self.__exchangeName = exchangeName
        self.__envelopeRepository = PersistedEnvelopeRepository() if envelopeRepository is None else envelopeRepository
        self.__groupRepository = RecipientGroupRepository() if groupRepository is None else groupRepository
        try:
            self.__connection = pika.BlockingConnection(parameters)
            self.__channel = self.__connection.channel()
            self.__channel.exchange_declare(exchangeName, 'topic')
            self.__channel.queue_declare(RabbitMqMessageBroker.MESSAGE_PERSISTENCE_QUEUE, False, True, False, False, None)
            self.__messageLogListener = MessageArchiver(self.__channel, exchangeName, envelopeRepository, RabbitMqMessageBroker.MESSAGE_PERSISTENCE_QUEUE, RabbitMqMessageBroker.MESSAGE_STATE_ROUTING_KEY)
        except Exception as e:
            raise Exception(e)

    def closeConnection(self):
        self.getConnection().close()
    def deleteQueue(self, queue):
        try:
            self.__channel.queue_delete(queue.getName())
        except Exception as e:
            raise Exception(e)


    # FOR TESTING ONLY
    def drainLoggingQueue(self):
        self.drainQueue(RabbitMqMessageBroker.MESSAGE_PERSISTENCE_QUEUE)
    def drainQueue(self, queue):
        queueName = queue.getName() if isinstance(queue, MessageQueue) else queue
        try:
            self.__channel.queue_purge(queueName)
        except Exception as e:
            raise Exception(e)

    def getChannel(self):
        return self.__channel

    def getConnection(self):
        return self.__connection

    def getMessageArchiver(self):
        return self.__messageLogListener

    def groupMembers(self, groupName):
        if groupName is None or (not groupName.endsWith(".*")):
            raise IllegalArgumentException("Invalid group name: " + groupName)

        oGroup = self.__groupRepository.findByName(groupName)
        if oGroup.isPresent():
            return oGroup.get().getGroupMembersAsList()
        return null

    def joinGroup(self, queueName, groupName):
        pass
        #if groupName is None or (not groupName.endsWith(".*")):
        #    raise IllegalArgumentException("Invalid group name: " + groupName)
        #if queueName is None:
        #    raise IllegalArgumentException("Null queueName")

        #oGroup = self.__groupRepository.findByName(groupName)
        #group = oGroup.get() if oGroup.isPresent() else RecipientGroup(groupName)
        #group.addMember(queueName)
        #self.__groupRepository.save(group)

    def listen(self, queue, consumer, timeout):
        if queue is None or consumer is None:
            raise IllegalArgumentException("Null arg")
        autoAck = False
        lastDeliveryTime = MessageBroker.now()
        
        #Start waiting for delivered messages, then consume them
        rmql = RabbitMqMessageBroker.RabbitMqListener(self.__channel, self, consumer, queue, autoAck)
        consumerTag = self.__channel.basic_consume(rmql.handleDelivery, queue.getName(), autoAck)
        
        thr = threading.Thread(target=self.__channel.start_consuming)
        thr.start()
        #Timeout loop: check if too much time has passed
        while(True):
            nowt = MessageBroker.now()
            print("Time", (nowt - lastDeliveryTime).total_seconds())
            if timeout > 0 and (nowt - lastDeliveryTime).total_seconds() >= timeout/1000:
                self.__channel.stop_consuming()
                thr.join()
                raise TimeLimitExceededException("After " + str(timeout) + " msec")
            #if rmql.isConsumed():
            #    break
            MessageBroker.sleep(100)
    
    def messageQueue(self, queueName, serviceName):
        routingKey = queueName
        queueName = RabbitMqMessageBroker.makeQueueName(serviceName, queueName)
        try:
            self.__channel.queue_declare(queueName, False, True, False, False, None)
            self.__channel.queue_bind(queueName, self.__exchangeName, routingKey)
        except Exception as e:
            raise Exception(e)
        ret = MessageQueue(queueName, serviceName, self)
        return ret
    
    def _receiveOne(self, queue, timeLimit):
        receivedEnvelope = None
        callTime = MessageBroker.now()
        try:
            response = None
            
            #Loop until we receive a message
            while True:
                autoAck = True
                response = self.__channel.basic_get(queue.getName(), autoAck)
                if response[0] is not None:
                    break
                #Did we time out?
                nowt = MessageBroker.now()
                if timeLimit > 0 and (nowt - callTime).total_seconds() >= timeLimit/1000:
                    #YES, throw an exception and exit
                    raise TimeLimitExceededException("After " + timeLimit + "msec")
                MessageBroker.sleep(RabbitMqMessageBroker.__WAIT_BETWEEN_POLLING_FOR_GET)
            body = response[2]
            #print("_receiveOne: "+ queue.getName())
            #print("_receiveOne: "+ str(body))
            receivedEnvelope = SimpleEnvelope()
            if body is not None:
                dct = json.loads(str(body, "UTF-8"))
                receivedEnvelope.deserialize(dct)
        except Exception as e:
            raise Exception(e)
        return receivedEnvelope

    def send(self, queue, message, expireTime):
        if queue is None or message is None:
            raise Exception("Null arg")
        if not isinstance(queue, MessageQueue) and len(queue) == 0:
            raise Exception("Null arg")
        return self._sendOne(queue, message, expireTime)
    
    def _sendOne(self, queue, message, expireTime):
        envelope = super(RabbitMqMessageBroker, self)._sendOne(queue, message, expireTime)
        #print("Env!" + str(envelope))
        try:
            jsons = json.dumps(envelope, default=SimpleEnvelope.serialize)
            properties = pika.BasicProperties(delivery_mode=2)
            routingKey = queue.getName() if isinstance(queue, MessageQueue) else queue
            #print("_sendOne: "+ routingKey)
            self.__channel.basic_publish(self.__exchangeName, routingKey, jsons, properties)
            return envelope
        except (TimeLimitExceededException, Exception) as e:
            raise Exception(e)

    def _setState(self, envelope, state):
        timestamp = super()._setState(envelope, state)
        stateChange = envelope.getId() + "@" + str(state) + "@" + timestamp
        self.__channel.basic_publish(self.__exchangeName, RabbitMqMessageBroker.MESSAGE_STATE_ROUTING_KEY, stateChange, None)
        return timestamp
