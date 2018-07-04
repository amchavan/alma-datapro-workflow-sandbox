# Data Processing Workflow Sandbox -- Message-passing RPC mode

The client (based on the _ExecutorClient_ class) sends a computing request message to a queue and awaits the response message. The message gets picked up by a computing server (based on the _Executor_ class) listening on that same queue, the response goes back directly to the original client.

Multiple clients and servers can share the same queue, which allows to distribuite the computational load to several executors

The following instructions assume your working directory is `alma-datapro-workflow-sandbox/pipes-and-filters`.

## Launcing the Fibonacci computation server

File _fib-executor.py_ computes the N-th Fibonacci number. It listens for requests coming on the _fibonacci_ queue on _localhost_, the body of the message will be interpreted as _N_.
Note that the implementation of the computation is deliberately very inefficient, do no attempt to submit requests with _N_>35 or so.

To launch it:
```
./fib-executor.py
```
## Requesting the N-th Fibonacci number

**TO-DO**