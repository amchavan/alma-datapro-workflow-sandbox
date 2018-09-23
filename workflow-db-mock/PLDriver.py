import json
import inspect
import importlib
import time

class Task():
    def __init__(self):
        pass

    def execute(self):
        raise NotImplementedError


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
        pass

    def handle(ex):
        if isinstance(ex, TaskException):
            return ex
        else:
            return TaskException("Unhandled exception", ex)


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


class Template():
    def __init__(self):
        #Read config file (from same dir)
        with open('config') as f:
            self.config = json.load(f)

    #Dynamically load module and class.
    #Check that it is actually a class.
    #Check that it inherits from 'Task' class.
    def loadClass(self, mod, cl):
        c = None
        try:
            m = importlib.import_module(mod)
        except ModuleNotFoundError as e:
            raise TaskException("Module '" + mod +"' not found.", e)
        try:
            c = inspect.getattr_static(m, cl)
            if not inspect.isclass(c):
                raise TaskException("Class '" + cl +"' not found in module '" + mod + "'.")
            if not issubclass(c, Task):
                raise TaskException("Class '" + cl +"' is not an instance of 'Task' class.")
            c = getattr(m, cl)
        except AttributeError as e:
            raise TaskException("Class '" + cl + "' not found in module '" + mod + "'.", e)
        if c is None:
            raise TaskException("Class '" + cl + "' failed to be loaded in an unexpected way.")
        return c

    #Iterate over tasks of task_type retrieving the appropriate task sub-class.
    #Then execute the task itself.
    def tasks(self, task_type, do=True):
        result = do
        tstats = {}
        if task_type not in self.config:
            return tstats, result
        for t in self.config[task_type]:
            ts = TaskStats(t['name'], task_type)
            tstats[t['name']] = ts
        if not do:
            return tstats, result
        for t in self.config[task_type]:
            ts = tstats[t['name']]
            try:
                ts.start(['comp','load'])
                cd = self.loadClass(t['module'], t['class'])
                ts.stop(['load'])
            except Exception as e:
                ts.fail(['comp','load'])
                ts.finish(['comp','load','exec'])
                ex = TaskException.handle(e)
                ts.setEx(ex)
                result = False
                continue
            try:
                ts.start(['exec'])
                c = cd()
                c.execute()
                ts.stop(['comp','exec'])
            except Exception as e:
                ts.fail(['comp','exec'])
                ts.finish(['comp','load','exec'])
                ex = TaskException.handle(e)
                ts.setEx(ex)
                result = False
                continue
            ts.finish(['comp','load','exec'])
        return tstats, result

    def preTasks(self, do=True):
        return self.tasks('pre-tasks')

    def postTasks(self, do=True):
        return self.tasks('post-tasks')


class PLDriver():
    def __init__(self, prj, ous, recipe):
        self.prj = prj
        self.ous = ous
        self.recipe = recipe
        self.temp = Template()

    def run(self):
        res = True
        stats = {}
        try:
            tstats, res = self.temp.preTasks(res)
            stats.update(tstats)
            ts = TaskStats('pipeline', 'pip')
            stats['pipeline'] = ts
            if res:
                pass
                #res = pipeline()
            (tstats, res) = self.temp.postTasks(res)
            stats.update(tstats)
        except Exception as e:
            res = False
            print("Exception handled")
            raise e
        for ts in tstats:
            tstats[ts].report()
