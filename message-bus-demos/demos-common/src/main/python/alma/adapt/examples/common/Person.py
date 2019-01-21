from adapt.messagebus.AbstractMessage import AbstractMessage

class Person(AbstractMessage):
    def __init__(self, name=None, age=None, alive=None):
        self.name = name
        self.age = age
        self.alive = alive
    def __str__(self):
        return self.__class__.__name__ + "[name=" + self.name + ", age=" + str(self.age) + ", alive=" + str(self.alive) + "]"
