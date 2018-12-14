from draws.messages.Message import Message
from draws.messages.Envelope import Envelope

class AbstractMessage(Message):
    def __init__(self):
        self.__envelope = Envelope()

    def getEnvelope(self):
        return self.__envelope

    def setEnvelope(self, envelope):
        self.__envelope = envelope

    def serialize(self):
        ret = self.__dict__.copy()
        if "_AbstractMessage__envelope" in ret:
            ret["_AbstractMessage__envelope"] = None
        return ret
    def deserialize(self, dct):
        self.__dict__ = dct.copy()
