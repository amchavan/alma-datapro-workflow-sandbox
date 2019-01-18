from draws.messages.AbstractMessage import AbstractMessage

class XTSSSetState(AbstractMessage):
    def __init__(self, fileType=None, cachedAt=None, name=None):
        self.fileType = fileType
        self.cachedAt = cachedAt
        self.name = name
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
        if self.fileType is None and obj.fileType is not None:
            return False
        if self.fileType is not None and not self.fileType == obj.fileType:
            return False
        if self.cachedAt is None and obj.cachedAt is not None:
            return False
        if self.cachedAt is not None and not self.cachedAt == obj.cachedAt:
            return False
        if self.name is None and obj.name is not None:
            return False
        if self.name is not None and not self.name == obj.name:
            return False
        return True
    def __str__(self):
        return self.__class__.__qualname__ + "[" + "fileType=" + str(self.fileType) + ", cachedAt=" + str(self.cachedAt) + ", name=" + str(self.name) + "]"
