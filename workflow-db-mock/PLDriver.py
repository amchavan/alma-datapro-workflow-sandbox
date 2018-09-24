#!/usr/bin/env python3

import os
import sys
import time
import json
import inspect
import argparse
import tempfile
import importlib
import traceback
import subprocess
import collections
sys.path.insert(0, "../shared")
import dbdrwutils
from dbmsgq import MqConnection, ExecutorClient

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

#Main class of the PLDriver implementation. It drives the execution
#of the pre tasks, the pipeline itself and the post tasks.
class PLDriver():
    def __init__(self, progID, ousUID, recipe):
        self._progID = progID
        self._ousUID = ousUID
        self._recipe = recipe
        #Initialization code for pipeline execution
        # Make sure we know where we are
        self.location = os.environ.get( 'DRAWS_LOCATION' )
        if self.location == None:
            raise RuntimeError( "DRAWS_LOCATION env variable is not defined" )

        # Make sure we know where the local replicated cache directory is
        self.replicatedCache = os.environ.get( 'DRAWS_LOCAL_CACHE' )
        if self.replicatedCache == None:
            raise RuntimeError( "DRAWS_LOCAL_CACHE env variable is not defined" )

        self.pipelineScript = "pipeline.py"
        self.thisDirectory = os.getcwd()		# Assume the pipeline script is in this same directory
        self.pipelineExecutable = self.thisDirectory + "/" + self.pipelineScript
        self.pipelineRunDirectory = tempfile.mkdtemp( prefix="drw-" )
        self.workingDirectory = tempfile.mkdtemp( prefix="drw-" )
        print( ">>> PipelineDriver: pipelineRunDirectory:", self.pipelineRunDirectory )
        print( ">>> PipelineDriver: workingDirectory:", self.workingDirectory )
        self.xtss = ExecutorClient( 'localhost', 'msgq', 'xtss' )
        self.mq   = MqConnection( 'localhost', 'msgq' )

        params = collections.OrderedDict()
        params['progID'] = self._progID
        params['ousUID'] = self._ousUID
        params['recipe'] = self._recipe
        params['location'] = self.location
        params['pipelineRunDir'] = self.pipelineRunDirectory
        params['replicatedCache'] = self.replicatedCache
        self._temp = Template(params)

    #In this method all the tasks are executed and are accounted for.
    def run(self):
        res = True
        cont = True
        stats = collections.OrderedDict()
        try:
            rep, res = self._temp.check()
            res = res or cont
            #print(rep)
            for r in rep:
                if len(rep[r]) > 2:
                    print(rep[r][0]+": "+str(len(rep[r])-1))
            tstats, res = self._temp.preTasks(res, cont)
            res = res or cont
            stats.update(tstats)
            ts = TaskStats('pipeline', 'pip')
            stats['pipeline'] = ts
            if res or cont:
                res = self._pipeline(ts)
            res = res or cont
            (tstats, res) = self._temp.postTasks(res, cont)
            res = res or cont
            stats.update(tstats)
        except Exception as e:
            res = False
            print("Exception handled")
            raise e
        for ts in stats:
            stats[ts].report()

    #Specific method to isolate the pipeline execution and error handling.
    def _pipeline(self, ts):
        res = True
        try:
            ts.start(['comp','exec'])
            completed = subprocess.run( [self.pipelineExecutable, self._progID, self._ousUID, self._recipe], cwd=self.pipelineRunDirectory)
            ret = completed.returncode
            if ret != 0:
                ts.fail(['comp','exec'])
                res = False
                print( ">>> PipelineDriver: Pipeline returned:", ret )
                if ret != 2:
                    raise RuntimeError( ">>> PipelineDriver: Pipeline returned: %d" % ret )
                # Pipeline returned 2 -- processing failed
                # Push the OUS to ProcessingProblem, and we're done
                dbdrwutils.setState( self.xtss, self._ousUID, "ProcessingProblem" )
        except Exception as e:
            res = False
            ts.fail(['comp','exec'])
            ex = TaskException.handle(e)
            ts.setEx(ex)
        ts.stop(['comp','exec'])
        return res

if __name__ == "__main__":
    from PLDriver import PLDriver
    parser = argparse.ArgumentParser(description='Pipeline Driver mock-up')
    parser.add_argument(dest="progID", help="ID of the project containing the OUS")
    parser.add_argument(dest="ousUID", help="ID of the OUS that should be processed")
    parser.add_argument(dest="recipe", help="Pipeline recipe to run")
    args=parser.parse_args()

    print(">>> PipelineDriver: progID=%s, ousUID=%s, recipe=%s" % (args.progID, args.ousUID, args.recipe))

    #Get from arguments
    driver = PLDriver(args.progID, args.ousUID, args.recipe)
    driver.run()
