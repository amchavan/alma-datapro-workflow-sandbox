import json

from threading import Thread

from adapt.messagebus.Envelope import State
from adapt.messagebus.SimpleEnvelope import SimpleEnvelope

from adapt.messagebus.rabbitmq.PersistedEnvelope import PersistedEnvelope

from adapt.messagebus.configuration.PersistedEnvelopeRepository import PersistedEnvelopeRepository

class MessageArchiver(Thread):
    def __init__(self, channel, exchangeName, envelopeRepository, mpq, msrk):
        super().__init__()
        self.__channel = channel
        self.__mpq = mpq
        self.__msrk = msrk
        self.__channel.queue_bind(self.__mpq, exchangeName, "#")
        self.__envelopeRepository = envelopeRepository
        #self.__consumer = MessageLogConsumer(channel, envelopeRepository)
    def handleDelivery(self, consumerTag, envelope, properties, body):
        message = str(body)
        msg = "delivered: " + envelope.routing_key + ": " + message
        #print(msg)
        persistedEnvelope = None
        if envelope.routing_key == self.__msrk:
            stateMessage = str(body)
            t = stateMessage.split("@")
            _id = t[0]
            state = t[1]
            timestamp = t[2]
            
            opt = self.__envelopeRepository.findByEnvelopeId(_id)
            persistedEnvelope = PersistedEnvelopeRepository() if opt is None else opt.get()
            state = State[state.split(".")[1]]
            persistedEnvelope.state = state
            if state == State.Sent:
                persistedEnvelope.sentTimestamp = timestamp
            elif state == State.Received:
                persistedEnvelope.receivedTimestamp = timestamp
            elif state == State.Consumed:
                persistedEnvelope.consumedTimestamp = timestamp
            elif state == State.Expired:
                persistedEnvelope.expiredTimestamp = timestamp
            elif state == State.Rejected:
                persistedEnvelope.rejectedTimestamp = timestamp
            else:
                raise Exception( "Unknown state: '" + state + "'" )
        else:
            simpleEnvelope = SimpleEnvelope()
            if body is not None:
                jsons = str(body, "UTF-8")
                simpleEnvelope.deserialize(json.loads(jsons))
            persistedEnvelope = PersistedEnvelope.convert(simpleEnvelope)
        self.__envelopeRepository.save(persistedEnvelope)
    def run(self):
        autoAck = True
        self.__channel.basic_consume(self.handleDelivery, self.__mpq, autoAck)
        self.__channel.start_consuming()
    def join(self):
        self.__channel.stop_consuming()
        super().join()
