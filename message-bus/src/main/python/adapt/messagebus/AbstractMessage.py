from adapt.messagebus.Message import Message
from adapt.messagebus.Envelope import Envelope

class AbstractMessage(Message):
    def __init__(self):
        self.envelope = Envelope()

    def getEnvelope(self):
        return self.envelope

    def setEnvelope(self, envelope):
        self.envelope = envelope

    def serialize(self):
        ret = self.__dict__.copy()
        if "envelope" in ret:
            del ret["envelope"]
        return ret
    def deserialize(self, dct):
        self.__dict__ = dct.copy()
