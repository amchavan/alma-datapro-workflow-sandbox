from draws.messages.AbstractMessage import AbstractMessage

class XTSSSetState(AbstractMessage):
    def __init__(self, ousUID=None, state=None):
        self.ousUID = ousUID
        self.state = state
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
        if self.state is None and obj.state is not None:
            return False
        if self.state is not None and not self.state == obj.state:
            return False
        return True
    def __str__(self):
        return self.__class__.__qualname__ + "[" + "ousUID=" + str(self.ousUID) + ", state=" + str(self.state) + "]"
