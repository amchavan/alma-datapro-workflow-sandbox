from draws.messages.AbstractMessage import AbstractMessage
from draws.messages.ResponseMessage import ResponseMessage

#Response: a doubled number
class DoubleResponse(AbstractMessage, ResponseMessage):
    #needed for JSON (de)serialization
    def __init__(self):
        super().__init__()
        self.doubled = None
