import json
import base64
import unittest
import threading

from threading import Thread

from draws.messages.Envelope import State
from draws.messages.Executor import Executor
from draws.messages.MessageBroker import MessageBroker
from draws.messages.ExecutorClient import ExecutorClient
from draws.messages.SimpleEnvelope import SimpleEnvelope
from draws.messages.RequestMessage import RequestMessage
from draws.messages.MessageConsumer import MessageConsumer
from draws.messages.ResponseMessage import ResponseMessage
from draws.messages.AbstractMessage import AbstractMessage
from draws.messages.RequestProcessor import RequestProcessor
from draws.messages.TimeLimitExceededException import TimeLimitExceededException

from draws.messages.security.JWTFactory import JWTFactory
from draws.messages.security.InvalidSignatureException import InvalidSignatureException

from draws.messages.rabbitmq.RabbitMqMessageBroker import RabbitMqMessageBroker
from draws.messages.rabbitmq.RecipientGroupRepository import RecipientGroupRepository
from draws.messages.rabbitmq.PersistedEnvelopeRepository import PersistedEnvelopeRepository

from draws.test.messages.TestUtils import TestMessage
from draws.test.messages.security.MockedTokenFactory import MockedTokenFactory


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
        except InvalidSignatureException as e:
            pass #no-op, expected
        except Exception as e:
            self.fail(str(e));

class TestJWTFactory(unittest.TestCase):
    def setUp(self):
        self.__tokenFactory = JWTFactory.getFactory()
    def testCreateStandardToken(self):
        token = self.__tokenFactory.create();
        self.assertIsNotNone(token)
        self.assertTrue(len(token) > 0)
        valid = self.__tokenFactory.isValid(token)
        self.assertTrue(valid)
        claims = self.__tokenFactory.decode(token)
        self.assertEqual(4, len(claims))
        self.assertEqual("user", claims["sub"])
        self.assertEqual("admin", claims["role"])
        exp = claims["exp"]
        self.assertTrue(exp > 10000)
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
    def testCreateInvalidToken(self):
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
            self.fail(str(e));

class TestTokenSecurityRabbitMQ(unittest.TestCase):
    __QUEUE_NAME = "test.queue"
    __EXCHANGE_NAME = "unit-test-exchange"

    def setUp(self):
        self.envelopeRepository = PersistedEnvelopeRepository()
        self.groupRepository = RecipientGroupRepository()
        self.broker = RabbitMqMessageBroker(exchangeName=TestTokenSecurityRabbitMQ.__EXCHANGE_NAME, envelopeRepository=self.envelopeRepository, groupRepository=self.groupRepository)
        self.queue = self.broker.messageQueue(TestTokenSecurityRabbitMQ.__QUEUE_NAME)
        self.brian = TestMessage("Brian May", 71, True)
        #Drain any existing messages in the logging queue
        self.broker.drainLoggingQueue();
        self.envelopeRepository.deleteAll();
        self.groupRepository.deleteAll();
        self.tokenFactory = MockedTokenFactory.getFactory();
        self.broker.setTokenFactory(self.tokenFactory);
    def tearDown(self):
        self.queue.delete
    def testSendSecureReceive(self):
        self.queue.send(self.brian)
        out = self.queue.receive()
        self.assertIsNotNone(out)
        self.assertEqual(State.Received, out.getState())
        self.assertIsNotNone(out.getReceivedTimestamp())
        self.assertEqual(self.brian, out.getMessage())
        self.assertEqual(out, out.getMessage().getEnvelope())
    def testSendSecureReject(self):
        inProps = {}
        inProps["valid"] = "false"
        token = self.tokenFactory.create(inProps)
        self.broker.setSendToken(token)
        self.queue.send(self.brian);
        messageLogListener = self.broker.getMessageLogListener()
        messageLogThread = messageLogListener
        messageLogThread.start();
        try:
           # time out right away because we
           # should see that the message was
           # rejected
           out = self.queue.receive(1000)
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
        self.queue.delete();

class TestExecutor(unittest.TestCase):
    __QUEUE_NAME = "test.executor.queue"
    __EXCHANGE_NAME = "unit-test-exchange"
    #Request: double a number
    class DoubleRequest(AbstractMessage, RequestMessage):
        #needed for JSON (de)serialization
        def __init__(self):
            super().__init__()
            self.number = None

    #Response: a doubled number
    class DoubleResponse(AbstractMessage, ResponseMessage):
        #needed for JSON (de)serialization
        def __init__(self):
            super().__init__()
            self.doubled = None

    #Doubles its input
    class Doubler(RequestProcessor):
        def process(self, message):
            request = message
            print(">>> Received request with number=" + str(request.number))
            response = TestExecutor.DoubleResponse()
            response.doubled = request.number + request.number
            return response

    class TestMessageConsumer(MessageConsumer):
        def __init__(self):
            self.doubled = None
        def consume(self, message):
            if not isinstance(message, TestExecutor.DoubleResponse):
                msg = "Not a " + TestExecutor.DoubleResponse.__qualname__ + ": " + str(message)
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
        self.queue = self.broker.messageQueue(TestExecutor.__QUEUE_NAME)
        self.broker.drainLoggingQueue()
        self.broker.drainQueue(self.queue.getName())
    def tearDown(self):
        self.broker.deleteQueue(self.queue)
    def doublerRunnable(self, doublerExecutor):
        try:
            doublerExecutor.run();
        except TimeLimitExceededException as e:
            print(">>> Timed out: " + str(e))
        except Exception as e:
            raise Exception(e)

    def testDoubler(self):
        doubler = TestExecutor.Doubler()
        doublerExecutor = Executor(self.queue, doubler, 5000)
        doublerThread = Thread(target=self.doublerRunnable, args=(doublerExecutor,))
        doublerThread.start()
        
        #Define the client for that Executor
        consumer = TestExecutor.TestMessageConsumer()
        client = ExecutorClient(self.queue, consumer)
        
        #Client sends a request to double 1
        request = TestExecutor.DoubleRequest()
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
