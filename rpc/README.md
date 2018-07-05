# Data Processing Workflow Sandbox -- Message-passing RPC mode

The client (based on the _ExecutorClient_ class) sends a computing request message to a queue and awaits the response message. The message gets picked up by a computing server (based on the _Executor_ class) listening on that same queue, the response goes back directly to the original client.

Note that the client provides a request identifier which gets echoed back by the server, so that the client can wait for the response to a specific request. This allows for multiple clients and servers sharing the same queue: requests are picked up by any available server and returned to the original client. 

Multiple clients and servers can share the same queue, which allows to distribuite the computational load to several executors

The following instructions assume your working directory is `alma-datapro-workflow-sandbox/pipes-and-filters`.

## Launcing the Fibonacci computation server

File _fib-executor.py_ computes the N-th Fibonacci number. The _Executor_ class listens for incoming requests on the _fibonacci_ queue on _localhost_; the body of the message will be interpreted as _N_.
Note that the implementation of the computation is deliberately very inefficient, do no attempt to submit requests with _N_>35 or so (unless you are prepared to be very patient, that is).

To launch it:
```
./fib-executor.py
```

## Requesting the N-th Fibonacci number

File _fib-client.py_ can be used to invoke computating the _N_-th Fibonacci number, the argument on the command line will be interpreted as _N_:
```
./fib-client.py 34
```


## A typical use case

As written above, a typical use case is that of multiple clients and servers sharing the same queue. It can be shown by opening two terminal windows, one for the servers and one for the clients.
Launch four servers the first window:
```
./fib-executor.py & 
./fib-executor.py & 
./fib-executor.py & 
./fib-executor.py &
```
In the second window, issue a number of parallel requests:
```
./fib-client.py 28 & 
./fib-client.py 29 &
./fib-client.py 30 & 
./fib-client.py 31 & 
./fib-client.py 33 & 
./fib-client.py 34 & 
./fib-client.py 35 & 
./fib-client.py 28 & 
```

You should see the requests being served asynchronously on the servers' console:

```
 [*] calling <function fib at 0x10f825f28> on 29 (pid=11670)
 [*] calling <function fib at 0x10d0fef28> on 28 (pid=11671)
 [*] calling <function fib at 0x104e1ff28> on 33 (pid=11669)
 [*] calling <function fib at 0x102531f28> on 30 (pid=11668)
 [*] calling <function fib at 0x10d0fef28> on 34 (pid=11671)
 [*] calling <function fib at 0x10f825f28> on 31 (pid=11670)
 [*] calling <function fib at 0x104e1ff28> on 35 (pid=11669)
```