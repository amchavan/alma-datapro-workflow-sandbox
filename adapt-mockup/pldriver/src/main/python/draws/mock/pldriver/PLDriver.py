#!/usr/bin/env python3

import os
import argparse
import tempfile
import subprocess
import collections

from draws.messages.rabbitmq.RabbitMqMessageBroker import RabbitMqMessageBroker

from draws.mock.pldriver.Template import Template
from draws.mock.pldriver.TaskStats import TaskStats
from draws.mock.pldriver.TaskException import TaskException
from draws.mock.messages.gen.XTSSSetState import XTSSSetState

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
        self._broker = RabbitMqMessageBroker()

        params = collections.OrderedDict()
        params['progID'] = self._progID
        params['ousUID'] = self._ousUID
        params['recipe'] = self._recipe
        params['location'] = self.location
        params['pipelineRunDir'] = self.pipelineRunDirectory
        params['replicatedCache'] = self.replicatedCache
        self._temp = Template(params)

    def __del__(self):
        self._broker.closeConnection()

    #In this method all the tasks are executed and are accounted for.
    def run(self):
        res = True
        cont = True
        stats = collections.OrderedDict()
        try:
            rep, res = self._temp.check()
            res = res or cont
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
                setState = XTSSSetState(self._ousUID, "ProcessingProblem")
                xtsspub = Publisher(self._broker, "xtss.transition")
                envelope = xtsspub.publish(setState)
        except Exception as e:
            res = False
            ts.fail(['comp','exec'])
            ex = TaskException.handle(e)
            ts.setEx(ex)
        ts.stop(['comp','exec'])
        return res

if __name__ == "__main__":
    from draws.mock.pldriver.PLDriver import PLDriver
    parser = argparse.ArgumentParser(description='Pipeline Driver mock-up')
    parser.add_argument(dest="progID", help="ID of the project containing the OUS")
    parser.add_argument(dest="ousUID", help="ID of the OUS that should be processed")
    parser.add_argument(dest="recipe", help="Pipeline recipe to run")
    args=parser.parse_args()

    print(">>> PipelineDriver: progID=%s, ousUID=%s, recipe=%s" % (args.progID, args.ousUID, args.recipe))

    #Get from arguments
    driver = PLDriver(args.progID, args.ousUID, args.recipe)
    driver.run()
