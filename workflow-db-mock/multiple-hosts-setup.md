# Data Reduction Workflow System mockup -- setup for multiple hosts

The following setup is for running the DRAWS mockup on two hosts,
one simulating the JAO installation and the other an ARC's. 
We'll call them _jao-host_ and _arc-host_, respectively.
* Data Tracker, Torque/Maui, Pipeline and the Pipeline Driver will be running at the ARC
* The replicated cache will run at both sites, transferring files from the ARC to JAO
* DRA, Oracle, XTSS, State System, AQUA/QA2, Product Ingestor and NGAS will be running at JAO

Both jao-host and arc-host must be configured in the same way, with Python, extra packages and a running CouchDB installation as described [in the main README file](README.md#prerequisites).

## Replication of CouchDB databases

The DRAWS mockup simulates one-way replication of Oracle and NGAS (JAO to ARC) and two-way replication of the message queue: all that is implemented using CouchDB and its (two-way) [replication feature](guide.couchdb.org/draft/replication.html). Notice the very first paragraph's security notice, you'll need to configure both servers to allow binding to any address.

There are several ways of setting up replication, a proven way is configuring a server to _pull_ changes over from the other server. Two-way replication can be set up by a pair of pull replications, on either server. If your server is running, the [Futon Web interface for replication](http://localhost:5984/_utils/#/replication) provides an easy replication setup form; alternatively, REST endpoints are available as well (see the guide).
 
Five tables need to be replicated from jao-host to arc-host, they represent Oracle tables and NGAS:
* _delivery-status_: (Oracle) 
* _ngas_: (NGAS) 
* _pipeline-reports_: (Oracle) 
* _products-metadata_: (Oracle) 
* _status-entities_: (Oracle)

Finally, table _msgq_ needs to be replicated two-way, from jao-host to arc-host and back. 

## Replicated cache

DRAWS uses one-way, ARC-to-JAO replication of the cache to transfer data products. The mockup uses an `rsync`-based implementation, like the Cycle 5/6 production system (probably Cycle 7 as well). You'll need to setup `ssh` permissions so that `rsync` can run without having to enter a password interactively. 

1. On jao-host, create an SSA certificate: `ssh-keygen -t rsa`, giving three empty answers to the prompts you'll see.<p>
1. Copy that certificate to arc-host: `ssh-copy-id -i ~/.ssh/id_rsa.pub arc-user@arc-host`, where _arc-user_ is the account you use on arc-host. You'll be prompted for the password.

As indicated on the console, you can now try `ssh arc-user@arc-host` and you should be able to login without having to enter any passwords. You should also be able to `rsync` from arc-host to jao-host. Create a dummy file on arc-host, for instance with `date>/tmp/q.txt`, then type on jao-host:
```
  rsync -r arc-user@arc-host:/tmp/q.txt .
  cat ./q.txt
```

See [here](https://ubuntuforums.org/showthread.php?t=238672) for a more detailed description.

## Environment and directories

Some environment variables should be set and directories created before running the mockup modules. 

On jao-host (make sure to substitute "arc-user" and "arc-host" with real values):
```
export DRAWS_LOCATION=JAO
export DRAWS_EU_CACHE=arc-user@arc-host:/tmp/draws-eu-cache
export DRAWS_JAO_CACHE=/tmp/draws-jao-cache
export DRAWS_LOCAL_CACHE=${DRAWS_JAO_CACHE}

rm -rf $DRAWS_LOCAL_CACHE 
mkdir -p $DRAWS_LOCAL_CACHE/weblogs
```

On arc-host:
```
export DRAWS_LOCATION=EU
export DRAWS_EU_CACHE=/tmp/draws-eu-cache
export DRAWS_LOCAL_CACHE=${DRAWS_EU_CACHE}

rm -rf $DRAWS_LOCAL_CACHE 
mkdir -p $DRAWS_LOCAL_CACHE/weblogs
```

## Resetting all data

If you need to restart from scratch, with no data in the database, the recommended option is to run the `clear-all-dbs.py` script on jao-host. _That script is highly destructive_ and will not ask for confirmation, use with care.

## Processes

**NOTE** The following processes should be launched _in their own terminal_ (or terminal tab) from the `.../workflow-db-mock` directory.

You will see all sorts of log messages on the terminals, but if you keep an eye on the dashboard Web page (see below) you should see the OUS moving from _ReadyForProcessing_ to _Processing_ etc. and on to the final  _Delivered_ state.

However, if the Pipeline reports a processing error (it should happen about one time in ten), the state will be _ProcessingProblem_. If that happens, restart the cycle by running the launcher again.

### On both jao-host and arc-host

The following steps should be performed on jao-host as well as arc-host:

* Launch the Web-based DRAWS dashboard:  
  `cd dashboard ; ./server.py`

* Launch the cache replicators:
  `./replicated-cache.py` on arc-host
  `./replicated-cache.py -euc $DRAWS_EU_CACHE` on arc-host

* Open a browser tab and visit `http://localhost:5000` and `http://localhost:8000/weblogs`


### On jao-host

The following steps should be performed on jao-host:

* `./launcher.py 2015.1.00657.S uid://X1/X1/Xb0`  
will create an OUS with ID=uid://X1/X1/Xb0 belonging to ObsProgram 2015.1.00657.S

* `./aqua-batch-helper.py`  
Will add the substate/recipe field to the OUS. Make sure it's a Pipeline recipe (look at the script's log or the Dashboard): if not, just repeat launching that OUS.

* `./dra.py -a EU`  
  launches the Data Reducer Assignment tool, assigning Pipeline jobs to the EU ARC.  
  It's an interactive tool and it should show the uid://X1/X1/Xb0 OUS. Copy and paste that UID in the terminal.  
  **NOTE** The transition from ReadyForProcessing to Processing only happens if the OUS is assigned to an Executive using the Data Reducer Assignment tool.

* `./xtss.py`  
  launches the XTSS mock

### On arc-host

The following steps should be performed on arc-host:

* `./torque-maui.py`  
  launches the Torque/Maui mock. Torque will launch the Pipeline driver, which in turn will launch the Pipeline.

* `./data-tracker.py` launches the Data Tracker running at the EU ARC.

### Back on jao-host

The following steps should be performed after getting back to jao-host:

* `./aqua-qa2`  
  launches AQUA/QA2. It's an interactive tool and it should show the uid://X1/X1/Xb0 OUS. Copy and paste that UID in the terminal and you will be shown the Pipeline report and asked for the QA2 score of that OUS. Type `P` for _Pass_.  
  **NOTE** The transition from ReadyForReview to Reviewing only happens if the OUS is reviewed using AQUA/QA2.

* To launch the JAO Product Ingestor running at JAO you'll
  need to redefine DRAWS_LOCAL_CACHE
  ```
  export DRAWS_LOCAL_CACHE=${DRAWS_JAO_CACHE}
  ./product-ingestor.py  
  ```


<!--
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
-->