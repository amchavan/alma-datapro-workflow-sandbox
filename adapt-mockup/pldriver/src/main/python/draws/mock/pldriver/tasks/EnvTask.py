from draws.mock.pldriver.Task import Task

class EnvLoader(Task):
    def execute(self, params):
        self._params = params
        self._checkNot(self._params, ['test'])
        self._params['test'] = 'test1'

class EnvChecker(Task):
    def execute(self, params):
        self._params = params
        self._check(self._params, ['test'])
        print(self._params['test'])
