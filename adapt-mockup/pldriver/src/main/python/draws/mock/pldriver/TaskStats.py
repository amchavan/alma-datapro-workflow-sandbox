import time

#Class to accumulate stats of the execution of the tasks.
class TaskStats():
    def __init__(self, name, task_type):
        self._name = name
        self._type = task_type
        self._done = {'load':False, 'exec':False, 'comp':False}
        self._fail = {'load':False, 'exec':False, 'comp':False}
        self._timer = {'load':0.0, 'exec':0.0, 'comp':0.0}
        self._start = {'load':0.0, 'exec':0.0, 'comp':0.0}
        self._stop = {'load':0.0, 'exec':0.0, 'comp':0.0}
        self._stopped = {'load':True, 'exec':True, 'comp':True}
        self._ex = None

    def start(self, ttype):
        for t in ttype:
            self._start[t] = time.time()
            self._stopped[t] = False
            self.done([t])

    def stop(self, ttype):
        for t in ttype:
            self._stop[t] = time.time()
            self._timer[t] = round(self._stop[t] - self._start[t], 5)
            self._stopped[t] = True

    def done(self, ttype):
        for t in ttype:
            self._done[t] = True

    def fail(self, ttype):
        for t in ttype:
            self._fail[t] = True

    def finish(self, ttype):
        for t in ttype:
            if not self._stopped[t]:
                self.stop([t])

    def setEx(self, ex):
        self._ex = ex

    def report(self):
        print("Task '" + self._name + "' report:")
        if self._done['comp']:
            if not self._fail['comp']:
                print("\tExecution: Success (" + str(self._timer['comp']) + "s)")
                print("\t\tLoad: Success (" + str(self._timer['load']) + "s)")
                print("\t\tExec: Success (" + str(self._timer['exec']) + "s)")
            elif self._fail['exec']:
                print("\tExecution: Failed (" + str(self._timer['comp']) + "s)")
                print("\t\tLoad: Success (" + str(self._timer['load']) + "s)")
                print("\t\tExec: Failed (" + str(self._timer['exec']) + "s)")
            elif self._fail['load']:
                print("\tExecution: Failed (" + str(self._timer['comp']) + "s)")
                print("\t\tLoad: Failed (" + str(self._timer['load']) + "s)")
                print("\t\tExec: Not Done (0.0s)")
            else:
                print("\tExecution: Unknown")
        else:
            print("\tExecution: Not Done (0.0s)")
        if self._ex is not None:
            print("\t\t\tException: " + self._ex.msg())
            self._ex.trace()
