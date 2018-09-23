import time
from PLDriver import Task

class APAFunc(Task):
    def __init__(self):
        pass
    def execute(self):
        print("Executing APAFunc task!")
        time.sleep(1)
