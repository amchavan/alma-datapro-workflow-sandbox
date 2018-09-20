# A Java Implementation of the Message Bus

This Eclipe project implements a simple CouchDB-based message bus in Java.
See this project's [class diagram](https://drive.google.com/file/d/18PNkMJEVu6y0roKZO_e_AKoYD0TLoPNn) for more info.

## Prerequisites

* A Java 8 environment including Maven
* A running instance of [CouchDB](couchdb.apache.org)

## Build

Clone the Git repository (`git clone https://github.com/amchavan/alma-datapro-workflow-sandbox.git`), change to the *message-bus* directory and run  
`mvn clean package`  
That will create two jarfiles in *target*, *message-bus-&lt;version&gt;.jar* and an executable jarfile called *message-bus-&lt;version&gt;-jar-with-dependencies.jar*

## Examples

The *alma.obops.draws.messages.examples* package includes a number of examples to show how the message bus can be used, usually by way of a message queue (`alma.obops.draws.messages.MessageQueue`).

To launch an example you can give its name as a command line argument to the jarfile, see below for more info.

All examples try to show something sensible on the console. Message listeners typically time out after some seconds of inactivity.

### Configuration

All examples assume that the CouchDB server is not secure and is accessible at `http://localhost:5984`. In oder to provide a different URL you'll need to provide a different `archive.couchdb.connection` property, for instance:

```bash
java -Darchive.couchdb.connection=http://ma24088.ads.eso.org:5984 ...
```

If your server is secured you wiĺl need to provide username and password as well. Use properties `archive.couchdb.user` and `archive.couchdb.passwd` for that.

See *CouchDbConfig.java* for more configuration options.

### Basic send and receive

Classes `BasicSender` and `BasicReceiver` exchange a simple message. Launch them as follows:
```bash
java -jar target/message-bus-0.0.1-SNAPSHOT-jar-with-dependencies.jar BasicReceiver &
java -jar target/message-bus-0.0.1-SNAPSHOT-jar-with-dependencies.jar BasicSender
```

### Basic execution

Classes `BasicExecutor` and `BasicExecutorClient` show how an RPC service (command/reply) can be implemented: the client asks for the current time in UT and the server returns that. You can launch a
server and severl clients as follows:
```
java -jar target/message-bus-0.0.1-SNAPSHOT-jar-with-dependencies.jar BasicExecutor &
java -jar target/message-bus-0.0.1-SNAPSHOT-jar-with-dependencies.jar BasicExecutorClient
java -jar target/message-bus-0.0.1-SNAPSHOT-jar-with-dependencies.jar BasicExecutorClient
java -jar target/message-bus-0.0.1-SNAPSHOT-jar-with-dependencies.jar BasicExecutorClient
```

### Calculator

The `Calculator` and `CalculatorClient` classes implement another RPC (command/reply) service. The server interprets a request like `{"service":"+","a":"1", "b":"2"}` as a *add 1 and 2 and return the result*; the client submits a number of requests. (The calculator has very limited capabilities!)

Launch client and server as follows:
```
java -jar target/message-bus-0.0.1-SNAPSHOT-jar-with-dependencies.jar Calculator &
java -jar target/message-bus-0.0.1-SNAPSHOT-jar-with-dependencies.jar CalculatorClient
```