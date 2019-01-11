#Interface for classes that wants to execute pre and post tasks.
class Task:
    def __init__(self):
        self._params = None
    def execute(self, params):
        raise NotImplementedError
    def getParams(self):
        return self._params
    def _check(self, params, cparams):
        if not all(p in params for p in cparams):
            raise TaskException("Params missing from '" + str(cparams) + "'. Current list is : '" + str(params.keys()) + "'")
    def _checkNot(self, params, cparams):
        if any(p in params for p in cparams):
            raise TaskException("Params present from '" + str(cparams) + "'. Current list is : '" + str(params.keys()) + "'")
