# Data Reduction Workflow System mockup -- setup for a single host

The following setup is for running the DRAWS mockup on a single machine, simulating:
* DRA, Torque/Maui, Pipeline and the Pipeline Driver running at the EU ARC
* a replicated cache, replicating from EU to JAO
* Oracle, XTSS, State System, AQUA/QA2, Product Ingestor, Data Tracker and NGAS (all remaining actors) running at JAO

### Directories

We need to mock a cache replicated from the EU arc to JAO, for which we'll need
to (re)create two directories:
```
export DRW_EU_CACHE=/tmp/eu-cache
export DRW_JAO_CACHE=/tmp/jao-cache
rm -rf $DRW_JAO_CACHE $DRW_EU_CACHE
mkdir -p $DRW_JAO_CACHE/weblogs $DRW_EU_CACHE/weblogs
```

But see also section _Resetting all data_ below.

### CouchDB setup

If you are restarting the system you should remove all entries from those databases. See also section _Resetting all data_ below.

### Resetting all data

If you need to restart from scratch, with no data in the caches or the database your best option is to run the `clear-all.sh` script. _This script is highly destructive_, use with care.

The script depends on the `DRW_EU_CACHE` and `DRW_JAO_CACHE` environment variables.

clear-all.sh can also be used to create the initial caches and databases.

### Processes

The following processes should be launched in their own terminal (or terminal tab).  
Make sure to `cd ..../workflow-db-mock` before you start; the *DRW_xxxx_CACHE* definitions should be set in your environment.

* `./pipeline-driver.py EU $DRW_EU_CACHE` launches the Pipeline driver; products will be copied to the EU cache

* `./replicated-cache.py -e EU -lc $DRW_EU_CACHE` launches the cache replicator running at the EU ARC; open a browser tab and visit `http://localhost:8000/weblogs`

* `./replicated-cache.py -e JAO -lc $DRW_JAO_CACHE -euc $DRW_EU_CACHE -p 8001` launches the cache replicator running at the JAO and replicating the cache from the EU ARC; open a browser tab and visit `http://localhost:8001/weblogs`

* `./xtss.py` launches the XTSS mock

* `./aqua-qa2` launches AQUA/QA2

* `./product-ingestor.py $DRW_JAO_CACHE` launches the JAO Product Ingestor, reading from the local replicating cache.

* `./data-tracker.py` launches the Data Tracker running at the EU ARC.

* `cd dashboard` and `./server.py` will launch the system dashboard Web application; open a browser tab and visit `http://localhost:5000`

* Finally,  
`./launcher.py 2015.1.00657.S uid://X1/X1/Xb0 PipelineCalibration EU`  
will create an OUS with ID=uid://X1/X1/Xb0 belonging to ObsProgram 2015.1.00657.S and launch the EU Pipeline on its 'data'

### Checking that all is OK

You will see all sorts of log messages on the terminals, but if you keep an eye on the dashboard Web page you should see the OUS moving from _ReadyForProcessing_ to _Processing_ to _ReadyForReview_.  
However, if the Pipeline reported a processing error (it should happen about one time in ten), the state will be _ProcessingProblem_. If that happens, just run the launcher again.)

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
