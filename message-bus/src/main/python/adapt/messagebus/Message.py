class Message(object):
    def getEnvelope(self):
        raise NotImplementedError
    def setEnvelope(self, envelope):
        raise NotImplementedError
