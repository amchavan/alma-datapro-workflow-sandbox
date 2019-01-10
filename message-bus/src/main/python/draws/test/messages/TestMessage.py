from draws.messages.AbstractMessage import AbstractMessage

class TestMessage(AbstractMessage):
    #Instance Methods
    def __init__(self, name=None, age=None, alive=None):
        self.name = name
        self.age = age
        self.alive = alive
    def serialize(self):
        ret = super().serialize()
        return ret
    def deserialize(self, dct):
        super().deserialize(dct)

    def __eq__(self, obj):
        return self.equals(obj)

    def equals(self, obj):
        if self is obj:
            return True
        if obj is None:
            return False
        if self.__class__ != obj.__class__:
            return False
        if self.age != obj.age:
            return False
        if self.alive != obj.alive:
            return False
        if self.name is None and obj.name is not None:
            return False
        if self.name is not None and not self.name == obj.name:
            return False
        return True

    def hashCode(self):
        prime = 31
        result = 1
        result = prime * result + self.age
        result = prime * result + (1231 if self.alive else 1237)
        result = prime * result + (0 if (self.name is None) else self.name.hashCode())
        return result
    
    def __str__(self):
        return "TestMessage[name=" + self.name + ", age=" + str(self.age) + ", alive=" + str(self.alive) + "]"
