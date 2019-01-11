import json
import inspect
import importlib

#Generic class, which has the knowledge of the pre and post tasks to execute.
#This information is loaded from a configuration file.
class Template():
    def __init__(self, params):
        self._params = params
        #Read config file (from same dir)
        with open('config') as f:
            self._config = json.load(f, object_pairs_hook=collections.OrderedDict)

    #Check that the configuration files is consistent and that it doesn't
    #have duplicate entries.
    def check(self):
        res = True
        tmp = []
        rep = collections.OrderedDict()
        if 'pre-tasks' in self._config:
            for t in self._config['pre-tasks']:
                if t['name'] not in rep:
                    rep[t['name']] = [t['name']]
                rep[t['name']].append(t)
        rep['pipeline'] = ['pipeline', {'name':'pipeline'}]
        if 'post-tasks' in self._config:
            for t in self._config['post-tasks']:
                if t['name'] not in rep:
                    rep[t['name']] = [t['name']]
                rep[t['name']].append(t)
        for r in rep:
            if len(rep[r]) > 2:
                res = False
        return rep, res

    #Dynamically load module and class. Check that it is actually a class.
    #Check that it inherits from 'Task' class.
    def _loadClass(self, mod, cl):
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
    def _tasks(self, task_type, do=True, cont=False):
        result = do
        tstats = collections.OrderedDict()
        if task_type not in self._config:
            return tstats, result
        for t in self._config[task_type]:
            ts = TaskStats(t['name'], task_type)
            tstats[t['name']] = ts
        if not do:
            return tstats, result
        for t in self._config[task_type]:
            ts = tstats[t['name']]
            try:
                ts.start(['comp','load'])
                cd = self._loadClass(t['module'], t['class'])
                ts.stop(['load'])
            except Exception as e:
                ts.fail(['comp','load'])
                ts.finish(['comp','load','exec'])
                ex = TaskException.handle(e)
                ts.setEx(ex)
                result = False
                if cont:
                    continue
                else:
                    break
            try:
                ts.start(['exec'])
                c = cd()
                c.execute(self._params)
                if c.getParams() is not None:
                    self._params = c.getParams()
                ts.stop(['comp','exec'])
            except Exception as e:
                ts.fail(['comp','exec'])
                ts.finish(['comp','load','exec'])
                ex = TaskException.handle(e)
                ts.setEx(ex)
                result = False
                if cont:
                    continue
                else:
                    break
            ts.finish(['comp','load','exec'])
        return tstats, result

    def preTasks(self, do=True, cont=False):
        return self._tasks('pre-tasks', do, cont)

    def postTasks(self, do=True, cont=False):
        return self._tasks('post-tasks', do, cont)

