from draws.messages.Envelope import Envelope;
from draws.messages.security import TokenFactory;

import pytz
import time
import socket
import datetime

class MessageBroker:
    DEFAULT_MESSAGE_BROKER_NAME = "alma";
    UT = pytz.utc
    #ISOTIMEDATESTRING_SHORT = "yyyy-MM-dd'T'HH:mm:ss";
    ISOTIMEDATESTRING_SHORT = "%Y-%m-%d'T'%H:%m:%S";

    @classmethod
    def now(cls):
        return datetime.datetime.now(tz=MessageBroker.UT)
    @classmethod
    def nowISO(cls):
        return datetime.datetime.strftime(MessageBroker.now(), MessageBroker.ISOTIMEDATESTRING_SHORT)
    @classmethod
    def ourIP(cls):
        ret = None
        try:
            ret = socket.gethostbyname(socket.gethostname())
        except Exception as e:
            print(e)
            ret = "0.0.0.0"
        return ret;
    @classmethod
    def parseIsoDatetime(cls, isoDatetime):
        dateFormat = SimpleDateFormat(ISOTIMEDATESTRING_SHORT)
        dateFormat.setTimeZone(UT)
        ret = dateFormat.parse(isoDatetime)
        return ret;

    @classmethod
    def sleep(cls, msec):
        time.sleep(msec/1000.0);
    def deleteQueue(self, queue):
        raise NotImplementedError
    def groupMembers(self, groupName):
        raise NotImplementedError
    def joinGroup(self, queueName, groupName):
        raise NotImplementedError
    def listen(self, queue, consumer, timeout):
        raise NotImplementedError

#    public void listen(queueName, consumer):
#        raise NotImplementedError

    def listenInThread(self, queue, consumer, timeout):
        raise NotImplementedError

    def messageQueue(self, queueName):
        raise NotImplementedError
        
    def receive(self, queue, timeLimit=0):
        raise NotImplementedError

    def send(self, queue, message, expireTime=0):
        raise NotImplementedError
    
    def setTokenFactory(self, factory):
        raise NotImplementedError
