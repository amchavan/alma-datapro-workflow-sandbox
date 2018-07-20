# Data Processing Workflow Sandbox -- System mockup

This is a mockup of the Data Processing Workflow system as shown [here](https://drive.google.com/file/d/11dwEzQyKbKvUbyV__czR1KUtYUBSSo7k/view?usp=sharing), demonstrating that it can be implemented as a asynchronous message-based system. It's based on a pipeline of components communicating via broadcasting messages plus a module (simulating the XTSS) implementing an RPC-like executor.

This version implements stages _ReadyForProcessing_, _Processing_, 
_ProcessingProblem_, _ReadyForReview_, _Reviewing_, _Verified_, _DeliveryInProgress_ of the
[OUS life-cycle](https://ictwiki.alma.cl/twiki/bin/view/ObsIF/ObsUnitSetLifeCycleJpg):

<img src="life-cycle.png" width="400">

## Prerequisites

* [Python 3.7.x](https://www.python.org/downloads/)
* A running instance of [CouchDB](couchdb.apache.org)
* The [Requests](http://docs.python-requests.org/en/master) Python package to communicate with the database

For this module, and in contrast to what is listed
[here](../README.md), you will not need the
[Pika package](https://pika.readthedocs.io/en/0.11.2/) or
[RabbitMQ 3.7.x](https://www.rabbitmq.com/)


## Modules

### The message bus

*TODO*
* Based on CouchDB
* It works
* *Notice* To reduce the amount of polling the bus uses a simple algorithm that extends the sleep intervals progressively. In practice that means, after a period of inactivity the message queue may take some time to react to new messages.


### CouchDB

Provides persistence to the system, supporting the message bus and mocking the State Archive, NGAS and various Oracle tables. Wrapped by the utilities in `shared/dbcon.py`, providing a vendor-agnostic API (?).
It's used in its default configuration.

### launcher.py

Temporary (?) component, creates status entities and launches a Pipeline run.  
Usage:  
`launcher.py [-h] progID ousUID recipe exec`

where _progID_ is the ID of the project containing the OUS, _ousUID_ is the ID of the OUS that should be processed,
_recipe_ is the Pipeline recipe (currently unused) and _exec_ is the Executive where this pipeline is running.  
For instance:  
`./launcher.py 2015.1.00657.S uid://X1/X1/Xb2 PipelineCalibration EU`

If needed, it creates a status entity for the OUS, then sends message to the Pipeline Driver on the `pipeline.process.EU` selector.

### pipeline-driver.py

Mocks the replacement for DARED.  
Usage:  
`pipeline-driver.py [-h] exec cache`  
where _exec_ is the executive where this driver is running ( one of 'EA', 'EU', 'JAO' or 'NA') and _cache_ is the absolute pathname of the replicating cache directory. For instance:  
`./pipeline-driver.py EU /tmp/EU`

It listens on the `pipeline.process.EU` selector and expects the message to include the Observing Program ID, OUS ID and the Pipeline processing recipe, for instance:  
`{"progID":"2015.1.00657.S", "ousUID":"uid://X1/X1/Xb2", "recipe":"PipelineCalibration"}`

When a message arrives, it starts the Pipeline by launching `pipeline.py` as a subprocess, and sets the OUS state to _ProcessingProblem_ if Pipeline processing failed. Otherwise:
* Copies the products directory to the replicating cache directory and sends a message to the JAO cache replicator on the `cached.JAO` selector -- meaning "pull this product directory to yourself".
* Copies the Weblog to the replicating cache directory and sends a message to the JAO *and* the local replicators, on selectors `cached.JAO` and `cached.EU` respectively -- meaning "pull this Weblog file to yourself". (If the current executive is JAO only one message is sent.)
* Sends the XML text of the pipeline report to AQUA at JAO on the `pipeline.report.JAO` selector.
* Finally, it sets the state of the OUS to _ReadyForReview_.


### pipeline.py

Mocks the ALMA Pipeline.  
Usage:  
`pipeline.py [-h] progID ousUID exec`  
where where _progID_ is the ID of the project containing the OUS, _ousUID_ is the ID of the OUS that should be processed and _exec_ is the Executive where the pipeline is running. For instance:  
`./pipeline.py 2015.1.00657.S uid://X1/X1/Xb2 EU`

It simulates processing by waiting a random interval, and 1 in 10 times (randomly) terminates with a processing error. Otherwise it create a products directory including a Weblog, a Pipeline report and a number of data products. For instance:
```
2015.1.00657.S_2018_07_19T07_10_03.781/
└── SOUS
    └── GOUS
        └── MOUS
            └── products
                ├── pl-report-X1-X1-Xa1-2018-07-19T07:10:03.781.xml
                ├── product-0-X1-X1-Xa1-2018-07-19T07:10:03.781.data
                ├── product-1-X1-X1-Xa1-2018-07-19T07:10:03.781.data
                ├── product-2-X1-X1-Xa1-2018-07-19T07:10:03.781.data
                ├── product-3-X1-X1-Xa1-2018-07-19T07:10:03.781.data
                ├── product-4-X1-X1-Xa1-2018-07-19T07:10:03.781.data
                └── weblog-X1-X1-Xa1-2018-07-19T07:10:03.781.zip
```

### replicated-cache.py

Implements an `rsync` based replicator, used to copy files from the ARCs to JAO. For instance, Pipeline product directories and Weblogs produced at the ARCs are copied to a local (ARC) cache, then replicated to JAO.  
Usage:
```
replicated-cache.py [-h] [-e EXEC] [-lc LCACHE] [-eac EACACHE]
  [--euc EUCACHE] [--nac NACACHE]
```
where _EXEC_ is the Executive where this cache driver is running, one of *EA*, *EU*, *JAO* or *NA*; _LCACHE_ is the absolute pathname of the local cache directory; *EACACHE* is the `rsync` location of the EA cache directory,  _username@host:dir_ (or simply _dir_);  *EUCACHE* for the EU cache dir and *NACACHE* for the NA cache directory. For instance:  
`./replicated-cache.py -e JAO -lc /tmp/local -euc /tmp/EU`

It expects the body of the request to be a JSON document:  
`{"fileType":"weblog", "cachedAt":"EU", "name": "weblog-X1-X1-Xa1-2018-07-19T07:10:03.781.zip"}`  
where _fileType_ can be _weblog_, _productsdir_, ...

It will then replicate the file or directory from the _cachedAt_ executive to JAO using the _XXCACHE_ spec given on the command line.
If the file type is Weblog, the zipped file is expanded and can be served to a browser by an embedded HTTP server, visiting for instance  `http://localhost:8000/weblog-X1-X1-Xa1-2018-07-19T07:10:03.781`

### xtss.py

Mock-up of the XTSS, an interface to the State System, providing no state transition checks.  
It implements an RPC server... (**TODO**)  
Usage:  
`./xtss.py`

It listens on selector _xtss_ and expects the body of the request to be a JSON document:
`{ "operation":"...", "ousUID":"uid://A003/X1/X1a", "value":"..."}`
where _operation_ can be one of _set-state_, _set-exec_, _set-exec_ , ...; and the meaning of _value_ depends on the command. For instance:  
`{"operation":"set-state", "ousUID":"uid://X1/X1/Xb2", "value":"ReadyForProcessing"}`  
Returns `201` (created) if all was well.

### aqua-qa2.py

Mock of AQUA QA2. Usage:  
`./aqua-qa2.py`  

A **background thread** listens on selector `pipeline.report.JAO` for requests to store a Pipeline report, and expects the message to be a JSON object including _ousUID_; _source_, the executive where the Pipeline was running; _report_, the report's XML text, BASE64-encoded; _timestamp_, the Pipeline run's timestamp; _productsDir_, the name of the products directory for that Pipeline run. For instance:
```
{
  "ousUID" : "uid://X1/X1/Xaf",
  "source" : "EU",
  "report" : "Cjw/eG1sIHZlcnNpb2..."
  "timestamp" : "2018-07-19T08:50:10.228",
  "productsDir": "2015.1.00657.S_2018_07_19T08_50_10.228"
}
```
When the message arrives a new entry is created in the `pipeline-reports` database.

In the **foreground**, a text-based user interface allows rudimentary QA2 review of a Pipeline execution. Once an OUS is selected its state is set to _Reviewing_ and the user can examine the Pipeline report and the Weblog. If the QA2 flag is set to _Fail_ the OUS state is reset to _ReadyForProcessing_.

Otherwise (QA2 flag _Pass_ or _Semipass_) OUS state is set to _Verified_ and a message is sent to the Product Ingestor on selector `ingest.JAO`.  Finally, the OUS state becomes _DeliveryInProgress_.

### product-ingestor.py

**NOT IMPLEMENTED**  
Mocks the Product Ingestor. Usage:  
`TODO`
where ...

It listens on selector `ingest.JAO` and expects the body of the request to be a JSON document:  
`{TODO}`  
where ...

### data-tracker.py

**NOT IMPLEMENTED**  
Mocks the Data Tracker. Usage:  
`TODO`
where ...

It listens on selector `TODO` and expects the body of the request to be a JSON document:  
`{TODO}`  
where ...




<!--
### aqua-qa0.py

Usage:  
```
./aqua-qa0.py [-h] uid
```
where _uid_ is the ID of an OUS, something like `uid://A003/X1/X1a`.

Simulates a QA astronomer setting the QA0 score for an OUS to "pass", using AQUA; it also plays the role of PLChecker by setting a random Pipeline recipe.
New OUSs will be silently created.

The module
1. injects the OUS into the system by setting its state to _ReadyForProcessing_
2. sets its Pipeline recipe to a random one
3. broadcasts the Pipeline recipe change to queue _pipe_, selector _recipe.change.&lt;recipe>_

**Note**: in Cycle 5 (and probably 6) state changes are distributed across multiple components. The QAA sets the score for an ExecBlock (not an OUS), and AQUA and the State System eventually set the state of the containing Member OUS to _FullyObserved_. The Data Tracker then changes that to _ReadyForProcessing_ when all data has been replicated to the SCO.

### dr-assign.py

Simulates astronomers running DRAssign at the regions.
Usage:
```
./dr-assign.py
```
The module listens to queue _pipe_, selector _recipe.change.&lt;recipe>_ and expects the body of the request to be a string including a pair of words `ousUID recipe`; for instance:  
`uid://A003/X1/X3 PipelineCombination`. It then selects a random Executive. After this stage the OUSStatus entity is fully populated and looks like:
```
{
  "_id": "uid___A003_X1_X1b",
  "_rev": "3-2a18c0a05e4aabc77e07758a7685e2f5",
  "entityId": "uid://A003/X1/X1b",
  "state": "ReadyForProcessing",
  "pipeline-recipe": "PipelineSingleDish",
  "executive": "JAO"
}
```
(Field `_rev_` is maintained by CouchDB.)

Finally, it broadcasts a Pipeline processing request to queue _pipe_, selector _pipeline.process.&lt;executive>_

### pipeline-driver.py

It mocks up the replacement for DARED (and the APA) running at JAO or one of the executives. Usage:  
```
./pipeline-driver.py exec
```
where _exec_ is one of EA, EU, JAO or NA. The module listens to queue _pipe_, selector _pipeline.process.&lt;exec>_ and expects the body of the request to be the ousUID. It then:
1. sets the OUS state to _Processing_
2. "Launches" the Pipeline and waits for it to finish (up to 5 seconds)
3. Sets the OUS state to _ProcessingProblem_ or _ReadyForReview_ (with some probability)
-->
