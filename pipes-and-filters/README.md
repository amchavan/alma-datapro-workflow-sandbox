# Data Processing Workflow Sandbox -- Pipes and filters

A ['pipes and filters'](https://docs.microsoft.com/en-us/azure/architecture/patterns/pipes-and-filters) pipeline made of components interacting by way of a message queue

We can define a multi-stage pipeline, with a data generator, one or more intermediate processing stages and a receiving end stage. All messages exchanged within the pipeline will be logged centrally to provide system monitoring.

The message-passing system will provide the Pipelines's "pipes", whereas the "filters" are built by way of the _Filter_ class in _shared/msgq.py_. The pipeline elements can be created as _send-only_, _receive-only_ or _send-receive_, depending on constructor parameters.

### System logger

Launch the system logger:
```
cd alma-datapro-workflow-sandbox
./rmq-filter-log-all.py -listen '#'
```
The logger will listen to all messages being exchanged and log them on the console.


### Feeding data into the pipeline

Data can be fed into the Pipeline by way of the `rmq-filter-send.py` script:
```
rmq-filter-send.py [-h] -send SEND message
```
where
* _SEND_ is the identifier of the filter (component) receiving the data, should be one of `stage2`, `stage3`, `stage4`, ...
* _message_ is the actual data, can be any string

For instance:
```
./rmq-filter-send.py -send stage2 msg00
```
will send `msg00` to the Pipeline's `stage2`.

### Adding Pipeline stages

Pipeline stages (filters) process the data they receive as inout and pass it on to the next stage -- in this case they simply double the input message (concatenate it to itself).

Stages can be added to the Pipeline with the `rmq-filter-pass-on.py` script:
```
rmq-filter-pass-on.py [-h] -listen LISTEN -send SEND
```
where
* _LISTEN_ is the identifier of the stage itself
* _SEND_ is the identifier of the next pipeline stage

For instance:
```
./rmq-filter-pass-on.py -listen stage2 -send stage3
```
will implement stage 2 of the pipeline, doubling its input and passing the result on to stage 3.

### End stage

The end stage of the Pipeline -- `rmq-filter-receive.py` -- simply logs its input to the console:
```
rmq-filter-receive.py [-h] -listen LISTEN
```
where _LISTEN_ is the identifier of the stage itself. For instance:
```
./rmq-filter-receive.py -listen stage4
```
will implement stage 4 of the pipeline as an end stage.


### Putting it all together

We will define a 4-stage pipeline, with two intermediate processing stages. We'll need four terminal windows where `alma-datapro-workflow-sandbox` is the working directory. Refer to the system logger window to see what is going on at all times.

Launch the processing stages 2 and 3 and the receiving end stage (in their own terminals):
```
./rmq-filter-pass-on.py -listen stage2 -send stage3
./rmq-filter-pass-on.py -listen stage3 -send stage4
./rmq-filter-receive.py -listen stage4
```

Finally, in yet another terminal generate the first data and feed it to the first processing stage:
```
./rmq-filter-send.py -send stage2 msg00
```

The system logger should display something like
```
 [x] stage2:msg00
 [x] stage3:msg00msg00
 [x] stage4:msg00msg00msg00msg00
```

Try feeding more data into the pipeline with `rmq-filter-send.py`; try also sending data directly to stages 3 and 4.

## Message-passing Remote Proceduce Call

TODO (battery is running out, damn... )
