from draws.messages.AbstractMessage import AbstractMessage

class PLReport(AbstractMessage):
    def __init__(self, ousUID=None, timestamp=None, source=None, report=None, productsDir=None):
        self.ousUID = ousUID
        self.timestamp = timestamp
        self.source = source
        self.report = report
        self.productsDir = productsDir
    def serialize(self):
        ret = super().serialize()
        return ret
    def deserialize(self, dct):
        super().deserialize(dct)
    def __eq__(self, obj):
        if self is obj:
            return True
        if obj is None:
            return False
        if self.__class__ != obj.__class__:
            return False
        if self.ousUID is None and obj.ousUID is not None:
            return False
        if self.ousUID is not None and not self.ousUID == obj.ousUID:
            return False
        if self.timestamp is None and obj.timestamp is not None:
            return False
        if self.timestamp is not None and not self.timestamp == obj.timestamp:
            return False
        if self.source is None and obj.source is not None:
            return False
        if self.source is not None and not self.source == obj.source:
            return False
        if self.report is None and obj.report is not None:
            return False
        if self.report is not None and not self.report == obj.report:
            return False
        if self.productsDir is None and obj.productsDir is not None:
            return False
        if self.productsDir is not None and not self.productsDir == obj.productsDir:
            return False
        return True
    def __str__(self):
        return self.__class__.__qualname__ + "[" + "ousUID=" + str(self.ousUID) + ", timestamp=" + str(self.timestamp) + ", source=" + str(self.source) + ", report=" + str(self.report) + ", productsDir=" + str(self.productsDir) + "]"
