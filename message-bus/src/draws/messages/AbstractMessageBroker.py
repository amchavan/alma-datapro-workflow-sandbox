from draws.messages.Envelope import State
from draws.messages.MessageBroker import MessageBroker
from draws.messages.SimpleEnvelope import SimpleEnvelope
from draws.messages.security.TokenFactory import TokenFactory

class AbstractMessageBroker(MessageBroker):
    def __init__(self):
        self._tokenFactory = None
        self._sendToken = None
        self.__ourIP = self.ourIP()

    def _computeState(self, envelope):
        timeToLive = envelope.getTimeToLive()
        if timeToLive == 0:
            self._setState(envelope, State.Expired)
            return
        #Should we reject the message?
        if self._isRejected(envelope):
            self._setState(envelope, State.Rejected)
            return
        #Got a valid envelope, mark it as Received 
        self._setState(envelope, State.Received)

    def deleteQueue(self, queue):
        pass #no-op

    def _initEnvelope(self, envelope):
        envelope.setSentTimestamp(MessageBroker.nowISO())
        envelope.setState(State.Sent)
        envelope.setMessageClass(envelope.getMessage().__class__.__module__ + "," + envelope.getMessage().__class__.__qualname__)
        envelope.setToken(self._sendToken)

    def _isRejected(self, envelope):
        #Is this broker secured? 
        if self._tokenFactory is None:
            return False; #Not secured, no rejection
        #Broker is secured: is the message signature valid?
        if not self._tokenFactory.isValid(envelope.getToken()):
            return True; #Invalid signature, reject
        
        #All is fine, no rejection
        return False;
    
    def listenInThread(self, queue, consumer, timeout):
        #receiver = () -> {    
        #    try {
        #        this.listen( queue, consumer, timeout );
        #    }
        #    catch ( TimeLimitExceededException e ) {
        #        // ignore
        #    }
        #    catch ( Exception e ) {
        #        throw new RuntimeException( e );
        #    }
        #};
        raise Exception
        t = Thread(receiver)
        t.start()
        return t

    def messageQueue(self, queueName):
        return MessageQueue(queueName, self)
    
    def receive(self, queue, timeLimit=0):
        #Wait for the first valid message we get and return it
        while True:
            envelope = self._receiveOne(queue, timeLimit)
            self._computeState(envelope)
            if envelope.getState() == State.Received or envelope.getState() == State.Rejected or envelope.getState() == State.Expired:
            #if envelope.getState() == State.Received:
                return envelope;
    
    def _receiveOne(self, queue, timeLimit):
        raise NotImplementedError

    def send(self, queue, message, expireTime=0):

        if queue is None or message is None:
            raise IllegalArgumentException( "Null arg" )
        
        #Are we sending to a group?
        if not queue.getName().endswith(".*"):
            ret = self._sendOne(queue, message, expireTime); #No, just send this message
            return ret;
        
        #We are sending to a group: loop over all recipients
        groupName = queue.getName();
        try:
            members = self.groupMembers(groupName)
            if members is None:
                raise Exception("Receiver group '" + groupName + "' not found")
            ret = None
            for member in members:
                memberQueue = MessageQueue(member, self)
                ret = self._sendOne(memberQueue, message, expireTime)
            return ret;
        except Exception as e:
            raise Exception(e)
    
    def _sendOne(self, queue, message, expireTime):
        envelope = SimpleEnvelope(message=message, originIP=self.__ourIP, queueName=queue.getName(), expireTime=expireTime)
        self._initEnvelope(envelope)
        return envelope
    
    #FOR TESTING ONLY
    def setSendToken(self, sendToken):
        self._sendToken = sendToken;

    def _setState(self, envelope, state):
        envelope.setState(state);
        now = MessageBroker.nowISO();
        if state == State.Received:
            envelope.setReceivedTimestamp(now)
        elif state == State.Sent:
            envelope.setSentTimestamp(now)
        elif state == State.Expired:
            envelope.setExpiredTimestamp(now)
        elif state == State.Rejected:
            envelope.setRejectedTimestamp(now)
        elif state == State.Consumed:
            envelope.setConsumedTimestamp(now)
        else:
            raise Exception("Unsupported state: " + state)
        return now;
    
    def setTokenFactory(self, factory):
        self._tokenFactory = factory
        self._sendToken = factory.create()
