import json
import base64
import unittest
import threading

from threading import Thread

from adapt.messages.Envelope import State
from adapt.messages.Executor import Executor
from adapt.messages.Publisher import Publisher
from adapt.messages.Subscriber import Subscriber
from adapt.messages.MessageBroker import MessageBroker
from adapt.messages.ExecutorClient import ExecutorClient
from adapt.messages.SimpleEnvelope import SimpleEnvelope
from adapt.messages.RequestMessage import RequestMessage
from adapt.messages.MessageConsumer import MessageConsumer
from adapt.messages.ResponseMessage import ResponseMessage
from adapt.messages.AbstractMessage import AbstractMessage
from adapt.messages.RequestProcessor import RequestProcessor
from adapt.messages.AbstractRequestMessage import AbstractRequestMessage
from adapt.messages.TimeLimitExceededException import TimeLimitExceededException

from adapt.messages.security.JWTFactory import JWTFactory

from adapt.messages.rabbitmq.RabbitMqMessageBroker import RabbitMqMessageBroker
from adapt.messages.configuration.RecipientGroupRepository import RecipientGroupRepository
from adapt.messages.configuration.PersistedEnvelopeRepository import PersistedEnvelopeRepository

from adapt.test.messages.TestMessage import TestMessage
from adapt.test.messages.DoubleRequest import DoubleRequest
from adapt.test.messages.DoubleResponse import DoubleResponse
from adapt.test.messages.security.MockedTokenFactory import MockedTokenFactory


class TestSerialization(unittest.TestCase):
    def testSimpleEnvelope(self):
        jimi = TestMessage("Jimi Hendrix", 28, False)
        _in = SimpleEnvelope(message=jimi, originIP="134.171.1.1", queueName="Q", expireTime=1000)
        jsons = json.dumps(_in, default=SimpleEnvelope.serialize)
        #print(">>> " + str(json))
        out = SimpleEnvelope()
        out.deserialize(json.loads(jsons))
        self.assertEqual(_in, out);

    #def testCouchDbEnvelope(self):
    #    jimi = TestMessage("Jimi Hendrix", 28, False)
    #    _in = CouchDbEnvelope(message=jimi, originIP="134.171.1.1", queueName="Q", expireTime=1000)
    #    _in.setVersion( "abcd" );
    #    json = _in.__dict__
    #    print(">>> " + str(json))
    #    out = CouchDbEnvelope()
    #    out.__dict__ = json
    #    self.assertEqual(_in, out);

class TestMockedTokenFactory(unittest.TestCase):
    def setUp(self):
        self.__tokenFactory = MockedTokenFactory.getFactory();
    def testCreateStandardToken(self):
        token = self.__tokenFactory.create()
        self.assertIsNotNone(token)
        self.assertTrue(len(token) > 0)
        valid = self.__tokenFactory.isValid(token)
        self.assertTrue(valid)
        claims = self.__tokenFactory.decode(token)
        self.assertEqual( 3, len(claims))
        self.assertEqual( "user",  claims["sub"])
        self.assertEqual( "admin", claims["role"])
        self.assertEqual( "10000", claims["ttl"])
    def testCreateValidToken(self):
        inProps = {}
        inProps["iss"] = "amchavan"
        inProps["role"] = "admin"
        token = self.__tokenFactory.create(inProps)
        self.assertIsNotNone(token)
        self.assertTrue(len(token) > 0)
        valid = self.__tokenFactory.isValid(token)
        self.assertTrue(valid)
        outProps = self.__tokenFactory.decode(token)
        self.assertTrue(inProps == outProps)
    def testCreateInvalidToken(self):
        inProps = {}
        inProps["valid"] = "false"
        token = self.__tokenFactory.create(inProps);
        self.assertIsNotNone(token);
        self.assertTrue(len(token) > 0)
        valid = self.__tokenFactory.isValid(token)
        self.assertFalse(valid)
        try:
            self.__tokenFactory.decode(token)
        except Exception as e:
            pass #no-op, exception is expected

class TestJWTFactory(unittest.TestCase):
    def setUp(self):
        self.__tokenFactory = JWTFactory()
    def testCreateStandardToken(self):
        token = self.__tokenFactory.create();
        self.assertIsNotNone(token)
        self.assertTrue(len(token) > 0)
        valid = self.__tokenFactory.isValid(token)
        self.assertTrue(valid)
        claims = self.__tokenFactory.decode(token)
        self.assertEqual(4, len(claims))
        self.assertEqual("user", claims["sub"])
        roles = claims.get("roles")
        self.assertTrue("OBOPS/AOD" in roles)
        ttl = claims[JWTFactory.TIME_TO_LIVE_CLAIM]
        self.assertTrue(ttl == JWTFactory.TIME_TO_LIVE)
        expires = claims[JWTFactory.EXPIRES_CLAIM]
        self.assertIsNotNone(expires)
    def testCreateValidToken(self):
        inProps = {}
        inProps["sub"] = "amchavan"
        inProps["role"] = "admin"
        token = self.__tokenFactory.create(inProps)
        self.assertIsNotNone(token)
        self.assertTrue(len(token) > 0)
        valid = self.__tokenFactory.isValid(token)
        self.assertTrue(valid)
        outProps = self.__tokenFactory.decode(token)
        self.assertEqual("amchavan", outProps["sub"])
        self.assertEqual("admin", outProps["role"])
    def estCreateInvalidToken(self):
        inProps = {}
        inProps["sub"] = "amchavan"
        token = self.__tokenFactory.create(inProps)
        self.assertIsNotNone(token)
        self.assertTrue(len(token) > 0)
        #now try to fudge the token
        t = token.split(".")
        encodedHeader    = t[0]
        encodedBody      = t[1]
        encodedSignature = t[2]
        print("AAAA_"+encodedBody+"_AAAA")
        body = base64.b64decode(encodedBody.encode()).decode()
        fudgedBody = body.replace("amchavan", "pjwoodhouse")
        encodedFudgedBody = base64.b64encode(fudgedBody.encode()).decode()
        fudgedToken =  encodedHeader + "." + encodedFudgedBody + "." + encodedSignature;
        valid = self.__tokenFactory.isValid(fudgedToken);
        self.assertFalse(valid)
        try:
            self.__tokenFactory.decode(fudgedToken)
        except InvalidSignatureException as e:
            pass #no-op, expected
        except Exception as e:
            pass #no-op, expected

class TestTokenSecurityRabbitMQ(unittest.TestCase):
    __QUEUE_NAME = "rock.stars"
    __SERVICE_NAME = "local"
    __EXCHANGE_NAME = "unit-test-exchange"

    def setUp(self):
        self.envelopeRepository = PersistedEnvelopeRepository()
        self.groupRepository = RecipientGroupRepository()
        self.broker = RabbitMqMessageBroker(exchangeName=TestTokenSecurityRabbitMQ.__EXCHANGE_NAME, envelopeRepository=self.envelopeRepository, groupRepository=self.groupRepository)
        #self.queue = self.broker.messageQueue(TestTokenSecurityRabbitMQ.__QUEUE_NAME)
        self.publisher = Publisher(self.broker, TestTokenSecurityRabbitMQ.__QUEUE_NAME)
        self.subscriber = Subscriber(self.broker, TestTokenSecurityRabbitMQ.__QUEUE_NAME, TestTokenSecurityRabbitMQ.__SERVICE_NAME)
        #Drain any existing messages in the logging queue
        self.broker.drainLoggingQueue();
        self.envelopeRepository.deleteAll();
        self.groupRepository.deleteAll();

        self.brian = TestMessage("Brian May", 71, True)
        #self.tokenFactory = MockedTokenFactory.getFactory();
        self.tokenFactory = JWTFactory()
        self.broker.setTokenFactory(self.tokenFactory);
    def tearDown(self):
        self.subscriber.getQueue().delete()
    def testSendSecureReceive(self):
        self.publisher.publish(self.brian)
        out = self.subscriber.receive()
        self.assertIsNotNone(out)
        self.assertEqual(State.Received, out.getState())
        self.assertIsNotNone(out.getReceivedTimestamp())
        self.assertEqual(self.brian, out.getMessage())
        self.assertEqual(out, out.getMessage().getEnvelope())
    def testSendSecureReject(self):
        #inProps = {}
        #inProps["valid"] = "false"
        #token = self.tokenFactory.create(inProps)
        #self.broker.setSendToken(token)
        #self.publisher.publish(self.brian);

        token = self.tokenFactory.create()
        self.broker.setSendToken(token[0:-2])
        e = self.publisher.publish(self.brian)
        messageLogListener = self.broker.getMessageArchiver()
        messageLogThread = messageLogListener
        messageLogThread.start();
        try:
           # time out right away because we
           # should see that the message was
           # rejected
           out = self.subscriber.receive(1000)
        except TimeLimitExceededException as e:
            pass #no-op, expected
        messageLogThread.join()
        #Give some time to the background thread to catch up
        MessageBroker.sleep(1000)
        _all = self.envelopeRepository.findAll()
        if _all is not None:
            for p in _all:
                state = p.asSimpleEnvelope().getState()
                print(">>> TestPersistence.envelope(): p: " + p + ", state: " + state);
                self.assertEqual(State.Rejected, state);

class TestExecutor(unittest.TestCase):
    __QUEUE_NAME = "test.executor.queue"
    __EXCHANGE_NAME = "unit-test-exchange"
    #Doubles its input
    class Doubler(RequestProcessor):
        def process(self, message):
            request = message
            print(">>> Received request with number: " + str(request.number))
            response = DoubleResponse()
            response.doubled = request.number + request.number
            return response

    class TestMessageConsumer(MessageConsumer):
        def __init__(self):
            self.doubled = None
        def consume(self, message):
            if not isinstance(message, DoubleResponse):
                msg = "Not a " + DoubleResponse.__qualname__ + ": " + str(message)
                print(">>>>> message 2: " + str(message))
                print(">>>>> Thread: " + threading.currentThread().getName())
                print(">>>>> " + msg)
                print(msg)
                raise Exception(msg)
            self.doubled = message.doubled

    def setUp(self):
        self.envelopeRepository = PersistedEnvelopeRepository()
        self.groupRepository = RecipientGroupRepository()
        self.broker = RabbitMqMessageBroker("amqp://localhost:5672", "guest", "guest", TestExecutor.__EXCHANGE_NAME, self.envelopeRepository, self.groupRepository)
        self.publisher = Publisher(self.broker, TestExecutor.__QUEUE_NAME)
        self.subscriber = Subscriber(self.broker, TestExecutor.__QUEUE_NAME, "test")
        self.broker.drainLoggingQueue()
        self.broker.drainQueue(self.subscriber.getQueue())
    def tearDown(self):
        self.broker.deleteQueue(self.subscriber.getQueue())
    def doublerRunnable(self, doublerExecutor):
        try:
            doublerExecutor.run();
        except TimeLimitExceededException as e:
            print(">>> Timed out: " + str(e))
        except Exception as e:
            raise Exception(e)

    def testDoubler(self):
        doubler = TestExecutor.Doubler()
        doublerExecutor = Executor(self.subscriber, doubler, 5000)
        doublerThread = Thread(target=self.doublerRunnable, args=(doublerExecutor,))
        doublerThread.start()
        
        #Define the client for that Executor
        consumer = TestExecutor.TestMessageConsumer()
        client = ExecutorClient(self.publisher, consumer)
        
        #Client sends a request to double 1
        request = DoubleRequest()
        consumer.doubled = None
        request.number = 1
        client.call(request)
        
        #MessageBroker.sleep(1500);
        self.assertIsNotNone(consumer.doubled)
        print(">>> Received reply with number: " + str(consumer.doubled))
        self.assertEqual(2, consumer.doubled)

        #Client sends a request to double 17
        consumer.doubled = None
        request.number = 17
        client.call(request);
        self.assertIsNotNone(consumer.doubled)
        print(">>> Received reply with number: " + str(consumer.doubled))
        self.assertEqual(34, consumer.doubled)
        doublerThread.join()
        
if __name__ == '__main__':
    from tests import TestExecutor
    unittest.main()
