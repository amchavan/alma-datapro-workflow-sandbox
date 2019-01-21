class TimeLimitExceededException(Exception):
    def __init__(self, message=None):
        super(TimeLimitExceededException, self).__init__(message)
