from adapt.messagebus.AbstractRequestMessage import AbstractRequestMessage

#Request: double a number
class DoubleRequest(AbstractRequestMessage):
    #needed for JSON (de)serialization
    def __init__(self):
        super().__init__()
        self.number = None
