# Data Processing Workflow Sandbox

This project is a playground for the new architecture of ALMA's Data Processing Workflow system, based on a message passing backbone (currently RabbitQM)
<!--
Place project URL here
-->
It defines 
* a ['pipes and filters' pipeline](pipes-and-filters/README.md)  made of components interacting by way of a message queue
* a [message-passing RPC mode](rpc/README.md), where the client sends a computing request message to the server's queue
* a mock-up of the [Data Processing Workflow support system](workflow-mock/README.md)

## Prerequisites
[Python 3.7.x](https://www.python.org/downloads/)
or higher,
[RabbitMQ 3.7.x](https://www.rabbitmq.com/)
or higher.

## Build
Clone the Git repository (`git clone https://github.com/amchavan/alma-datapro-workflow-sandbox.git`).

The current version of this sandbox does not require an explicit build stage.

## RabbitMQ server

Start the RabbitMQ server (from any location):
```
rabbitmq-server
```
