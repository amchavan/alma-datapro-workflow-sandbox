# Two Java Implementations of the Message Bus

This Eclipe project implements defines two Java implementations of the message broker, based on [CouchDB](couchdb.apache.org) and [RabbitMQ](https://www.rabbitmq.com).
See this project's [class diagram](https://drive.google.com/file/d/18PNkMJEVu6y0roKZO_e_AKoYD0TLoPNn) for more info.

## Prerequisites

* A Java 8 environment including Maven
* A running instance of [CouchDB](couchdb.apache.org)
* A running instance of [RabbitMQ](https://www.rabbitmq.com)

## Build

Clone the Git repository (`git clone https://github.com/amchavan/alma-datapro-workflow-sandbox.git`), change to the *message-bus* directory and run  
`mvn clean install`  
That will create (in *target*) and install (in the local Maven repo) jarfile
*message-bus-&lt;version&gt;.jar* 
After that, run
`pip3 install --upgrade --user src/main/python/`  
to install message-bus to local Python installation area.

## Examples

See the [message-bus-demos module](../message-bus-demos/README.md).
