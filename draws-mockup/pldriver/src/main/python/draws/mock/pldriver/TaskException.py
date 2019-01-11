import traceback

#Generic exception for problems detected during tasks execution.
class TaskException(Exception):
    def __init__(self, msg, ex=None):
        super().__init__(msg)
        self._ex = ex
        self._msg = msg

    def msg(self):
        if self._ex:
            return "TaskException: " + self.__str__() + " -- " + self._ex.__class__.__name__ + ": " + self._ex.__str__()
        return "TaskException: " + self.__str__()

    def trace(self):
        #print(self.__traceback__)
        if self.__traceback__ is not None:
            traceback.print_tb(self.__traceback__)

    def handle(ex, msg=None):
        if isinstance(ex, TaskException):
            return ex
        elif msg is None:
            ex2 = TaskException("Unhandled exception", ex)
            ex2.__traceback__ = ex.__traceback__
            return ex2
        else:
            ex2 = TaskException(msg, ex)
            ex2.__traceback__ = ex.__traceback__
            return ex2

