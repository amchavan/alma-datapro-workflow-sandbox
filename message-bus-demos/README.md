# Java Message Bus Demos

These Eclipe projects implement example usages of the 
[RabbitMQ](https://www.rabbitmq.com)-based message broker.

The *alma.obops.draws.messages.examples* package includes a number of demos to show how the message bus can be used.

All examples try to show something sensible on the console. Message subscribers typically time out after some seconds of inactivity.

## Prerequisites

* A Java 8 environment including Maven
* A running instance of [CouchDB](couchdb.apache.org)
* A running instance of [RabbitMQ](https://www.rabbitmq.com)

## Build

Make sure you have built the [message bus](../README.md) first.

Then: if needed, clone the Git repository (`git clone https://github.com/amchavan/alma-datapro-workflow-sandbox.git`), change to the *message-bus-demos* directory and run  
`mvn clean package`  
That will create several jarfiles in the *target* subdirectories, for instance *basic-sender/target/basic-sender-&lt;version&gt;.jar*

**NOTE** The following examples assume the current working directory is *message-bus-demos*.

## Configuration

All demos depend on configuration parameters in *$ACSDATA/config/archiveConfig.properties*.  

### RabbitMQ server

Property `archive.rabbitmq.connection` should be set to something like  `http://localhost:5984`.

The server itself should _not_ be secured, or you'll need to provide username and password as well. Use properties `archive.rabbitmq.username` and `archive.rabbitmq.password` for that.

See *RabbitMqConfigurationProperties.java* for more configuration options.

### Relational server

## Basic send and receive

Modules `basic-sender` and `basic-receiver` exchange a simple message. Launch them as follows, on two separate terminal windows:

```bash
java -jar basic-receiver/target/basic-receiver-*.jar qname=example.queue sname=receiver
java -jar basic-sender/target/basic-sender-*.jar qname=example.queue
```

Parameter `qname` identifies the queue a message will be published to. Parameter `sname` identifies which service is subscribing — there may be multiple subscribers for the same message, see below.

If you list the RabbitMQ queues (with `rabbitmqctl list_queues`) after they both terminate you'll see that queue _receiver.example.queue_ was created and is empty, because a message was sent and received. 

Queue _message.persistence.queue_ was also created, see below for more info.

## Publish and subscribe

Modules `sender` and `receiver` implement a simple published/subscriber pair. In the simplest configuration, you can launch a single pair, as follows (and in two separate terminal windows):

```bash
java -jar receiver/target/receiver-*.jar qname=example.queue sname=receiver
java -jar sender/target/sender-*.jar qname=example.queue
```

That would be equivalent to the previous demo. However, you can publish multiple messages with optional args _repeats_ and _delay_ (delay in seconds between repeats), for instance:  
`java -jar sender/target/sender-*.jar qname=example.queue repeats=10 delay=1`

You can also start a second subscriber (in a new terminal window) with the same parameters: each subscriber will receive half of the messages. (Use case: providing more resources for a compute-intensive job.)

Finally, you can start a second subscriber but with a different service name, for instance:  
`java -jar receiver/target/receiver-*.jar qname=example.queue sname=receiver2`

In this case both subscribers will receive all messages. (Use case: broadcasting a message.)

### Request/reply — Remote Procedure Call

Modules `rpc-client` and `rpc-server` show how an RPC service (request/reply) can be implemented: the client (_ExecutorClient_) asks for the current time in UT and the server (_Executor_) returns that. You can launch a server and a client as follows:

```bash
java -jar rpc-server/target/*.jar &
java -jar rpc-client/target/*.jar
```

Note that the _rpc-client_ accepts optional _repeats_ and _delay_ command-line args.