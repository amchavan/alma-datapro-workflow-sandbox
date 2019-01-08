# Two Java Implementations of the Message Bus

This Eclipe project implements defines two Java implementations of the message broker, based on [CouchDB](couchdb.apache.org) and [RabbitMQ](https://www.rabbitmq.com).
See this project's [class diagram](https://drive.google.com/file/d/18PNkMJEVu6y0roKZO_e_AKoYD0TLoPNn) for more info.

## Prerequisites

* A Java 8 environment including Maven
* A running instance of [CouchDB](couchdb.apache.org)
* A running instance of [RabbitMQ](https://www.rabbitmq.com)

## Build

Clone the Git repository (`git clone https://github.com/amchavan/alma-datapro-workflow-sandbox.git`), change to the *message-bus* directory and run  
`mvn clean package`  
That will create two jarfiles in *target*, *message-bus-&lt;version&gt;.jar* and an executable jarfile called *message-bus-&lt;version&gt;-jar-with-dependencies.jar*

## Examples

See the _message-bud-demos_ module.