#!/usr/bin/env python3

import os
import argparse
from PLDriver import PLDriver

# Mock-up of the Pipeline Driver (successor of DARED)
# It pretends to be running at one of the executives
if __name__ == "__main__":
    doStuff = False
    parser = argparse.ArgumentParser(description='Pipeline Driver mock-up')
    parser.add_argument(dest="progID", help="ID of the project containing the OUS")
    parser.add_argument(dest="ousUID", help="ID of the OUS that should be processed")
    parser.add_argument(dest="recipe", help="Pipeline recipe to run")
    args=parser.parse_args()

    print(">>> PipelineDriver: progID=%s, ousUID=%s, recipe=%s" % (args.progID, args.ousUID, args.recipe))

    #Get from arguments
    driver = PLDriver(args.progID, args.ousUID, args.recipe)
    driver.run()
