from adapt.messagebus.AbstractMessage import AbstractMessage
from adapt.messagebus.ResponseMessage import ResponseMessage

#Response: a doubled number
class DoubleResponse(AbstractMessage, ResponseMessage):
    #needed for JSON (de)serialization
    def __init__(self):
        super(DoubleResponse, self).__init__()
        self.doubled = None
