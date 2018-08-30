# Data Reduction Workflow System mockup -- setup for a single host

The following setup is for running the DRAWS mockup on a single machine, simulating:
* DRA, Torque/Maui, Pipeline and the Pipeline Driver running at the EU ARC
* a replicated cache, from EU to JAO
* Oracle, XTSS, State System, AQUA/QA2, Product Ingestor, Data Tracker and NGAS (all remaining actors) running at JAO

## Environment

Some environment variables should be set before running the mockup modules:
```
export DRAWS_LOCATION=EU
export DRAWS_EU_CACHE=/tmp/draws-eu-cache
export DRAWS_JAO_CACHE=/tmp/draws-jao-cache
export DRAWS_LOCAL_CACHE=${DRAWS_EU_CACHE}
```

## Directories

We need to mock a cache replicated from the EU arc to JAO, for which we'll need
to (re)create two directories:
```
rm -rf $DRAWS_JAO_CACHE $DRAWS_EU_CACHE
mkdir -p $DRAWS_JAO_CACHE/weblogs $DRAWS_EU_CACHE/weblogs
```

## Resetting all data

If you need to restart from scratch, with no data in the caches or the database your best option is to run the `clear-all-dbs.py` script. _That script is highly destructive_ and will not ask for confirmation, use with care.

### Processes

**NOTE** The following processes should be launched _in their own terminal_ (or terminal tab) from the `workflow-db-mock` directory.

You will see all sorts of log messages on the terminals, but if you keep an eye on the dashboard Web page (see below) you should see the OUS moving from _ReadyForProcessing_ to _Processing_ etc. and on to the final  _Delivered_ state.

However, if the Pipeline reports a processing error (it should happen about one time in ten), the state will be _ProcessingProblem_. If that happens, restart the cycle by running the launcher again.


* Launch the Web-based DRAWS dashboard:  
  `cd dashboard ; ./server.py`
  Now open a browser tab and visit `http://localhost:5000`

* `./launcher.py 2015.1.00657.S uid://X1/X1/Xb0`  
will create an OUS with ID=uid://X1/X1/Xb0 belonging to ObsProgram 2015.1.00657.S

* `./aqua-batch-helper.py`  
Will add the substate/recipe field to the OUS. Make sure it's a Pipeline recipe (look at the script's log): if not, just repeat launching that OUS.

* `./dra.py` launches the Data Reducer Assignment. It's an interactive tool and it should show the uid://X1/X1/Xb0 OUS. Copy and paste that UID in the terminal.  
  **NOTE** The transition from ReadyForProcessing to Processing only happens if the OUS is assigned to an Executive using the Data Reducer Assignment tool.

* `./xtss.py` launches the XTSS mock

* `./torque-maui.py` launches the Torque/Maui mock. It will launch the Pipeline driver, which in turn will launch the Pipeline.

* `./replicated-cache.py` launches the cache replicator running at the EU ARC; open a browser tab and visit `http://localhost:8000/weblogs`

* Redefine DRAWS_LOCAL_CACHE and DRAWS_LOCATION, then launch `./replicated-cache.py` again to mock the cache replicator running at the JAO:
  ```
  export DRAWS_LOCAL_CACHE=${DRAWS_JAO_CACHE}
  export DRAWS_LOCATION=JAO
  ./replicated-cache.py -p 8001 -euc $DRAWS_EU_CACHE 
  ```
  You can now open a browser tab and visit `http://localhost:8001/weblogs`

* `./aqua-qa2` launches AQUA/QA2. It's an interactive tool and it should show the uid://X1/X1/Xb0 OUS. Copy and paste that UID in the terminal and you will be shown the Pipeline report and asked for the QA2 score of that OUS. Type `P` for _Pass_.  
  **NOTE** The transition from ReadyForReview to Reviewing only happens if the OUS is reviewed using AQUA/QA2.

* To launch the JAO Product Ingestor running at JAO you'll
  need to redefine DRAWS_LOCAL_CACHE
  ```
  export DRAWS_LOCAL_CACHE=${DRAWS_JAO_CACHE}
  ./product-ingestor.py  
  ```

* `./data-tracker.py` launches the Data Tracker running at the EU ARC.


### Checking that all is OK

Now you can go to the AQUA/QA2 terminal, hit the Return key a couple of times if needed, and see that uid://X1/X1/Xb0 is in the list of "OUSs ready to be reviewed". Copy and paste that UID at the "Please enter..." prompt and you should be presented with the Pipeline repot (raw XML text) and the URL of the Weblog. Paste that into a Browser tab and you should see the 'Weblog' for that Pipeline execution.  
The Dashboard tab should show the OUS in the _Reviewing_ state.

If you enter [P]ass or [S]emipass at the prompt you should see the OUS going first to _Verified_, then _DeliveryInProgress_.  
If you enter [F]ail it should go to _ReadyForProcessing_.  
If you enter [C]ancel is should show you the list of OUSs again.

At this point you can go to the Dashboard page and verify that:

* The OUS state becomes _Verified_, then _DeliveryInProgress_
  * The OUS substate becomes _IngestionTriggered_, ... until it completes at _ProductsIngested_

* The OUS state becomes _Delivered_

* [The EU cache](http://localhost:8000/) at `http://localhost:8000` should show you a products directory (which you can navigate down to the bottommost _products_ directory and view its contents) and a zipped Weblog for that Pipeline execution.  [The EU weblogs cache](`http://localhost:8000/weblogs/`) should show an expanded Weblog.

* [The JAO cache](http://localhost:8001) at `http://localhost:8001` should be identical to the EU one

* Tables _Pipeline reports_, _NGAS documents_, _Products metadata_ and _Delivery status_ of the dashboard should display consistent information:
  * One Pipeline report and one Delivery status
  * Six files in NGAS (5 data products and one ZIP file)
  * Five product metadata records
  * Zero unread messages

If you prefer not to use the Dashboard you can query the database from the command line instead. For instance:  
`curl -H "Content-Type: application/json localhost:5984/pipeline-reports/_all_docs`

### Going to the movies

If you made it this far and everything was OK you may try to pretend you are watching the system working in real time: type the following commands into the launcher terminal, then immediately switch to the dashboard tab and watch the OUSs moving down their life-cycle:
```
./launcher.py 2015.1.00657.S uid://X1/X1/Xb1 PipelineCalibration EU ; sleep 2
./launcher.py 2015.1.00657.S uid://X1/X1/Xb2 PipelineCalibration EU ; sleep 2
./launcher.py 2015.1.00657.S uid://X1/X1/Xb3 PipelineCalibration EU ; sleep 2
./launcher.py 2015.1.00657.S uid://X1/X1/Xb4 PipelineCalibration EU ; sleep 2
./launcher.py 2015.1.00657.S uid://X1/X1/Xb5 PipelineCalibration EU ; sleep 2
./launcher.py 2015.1.00657.S uid://X1/X1/Xb6 PipelineCalibration EU ; sleep 2
./launcher.py 2015.1.00657.S uid://X1/X1/Xb7 PipelineCalibration EU ; sleep 2
./launcher.py 2015.1.00657.S uid://X1/X1/Xb8 PipelineCalibration EU ; sleep 2
./launcher.py 2015.1.00657.S uid://X1/X1/Xba PipelineCalibration EU
```

Note that the step from _ReadyForReview_ to _Reviewing_ is manual and requires AQUA/QA2 (see above). After that, [P]ass or [S]emipass OUSs should proceed automatically until  reaching _Delivered_.
