import json

from threading import Thread

class PersistenceListener(Thread):
    def __init__(self, channel, exchangeName, envelopeRepository, mpq, msrk):
        super().__init__()
        self.__channel = channel
        self.__mpq = mpq
        self.__msrk = msrk
        self.__channel.queue_bind(self.__mpq, exchangeName, "#")
        #self.__consumer = MessageLogConsumer(channel, envelopeRepository)
    def handleDelivery(self, consumerTag, envelope, properties, body):
        message = str(body)
        msg = ">>>> handling delivery: " + envelope.getRoutingKey() + ": " + message
        print(msg)
        persistedEnvelope = None
        if envelope.getRoutingKey().equals(self.__msrk):
            stateMessage = body
            t = stateMessage.split("@")
            _id = t[0]
            state = t[1]
            timestamp = t[2]
            
            _all = envelopeRepository.findAll()
            for pe in _all:
                print( ">>> " + pe );
            
            opt = envelopeRepository.findByEnvelopeId(_id)
            persistedEnvelope = opt.get()
            persistedEnvelope.state = state
            if state.name == "Sent":
                persistedEnvelope.sentTimestamp = timestamp
            elif state.name == "Received":
                persistedEnvelope.receivedTimestamp = timestamp
            elif state.name == "Consumed":
                persistedEnvelope.consumedTimestamp = timestamp
            elif state.name == "Expired":
                persistedEnvelope.expiredTimestamp = timestamp
            elif state.name == "Rejected":
                persistedEnvelope.rejectedTimestamp = timestamp
            else:
                raise Exception( "Unknown state: '" + state + "'" )
        else:
            simpleEnvelope = SimpleEnvelope()
            if body is not None:
                jsons = str(body, "UTF-8")
                print(">>>> delivered json: " + jsons)
                simpleEnvelope.deserialize(json.loads(jsons))
            persistedEnvelope = PersistedEnvelope.convert(simpleEnvelope)
        envelopeRepository.save(persistedEnvelope)
        _all = envelopeRepository.findAll()
        for pe in _all:
            print(">>> pe: " + pe)
    def run(self):
        autoAck = True;
        self.__channel.basic_consume(self.handleDelivery, self.__mpq, autoAck)
        self.__channel.start_consuming()
