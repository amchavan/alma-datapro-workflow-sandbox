import pika
import uuid
import time
import os

# Utility classes and functions for dealing with a message queue -- the implementation
# is specific to RabbitMQ (and the API too, I'm afraid).
#
# A M Chavan, 30-Jun-2018


class Filter():
	"""
		Implements a filter of the 'Pipes and filters' pattern: see
			https://docs.microsoft.com/en-us/azure/architecture/patterns/pipes-and-filters
		Implementation is specific to RabbitMQ (the pipe) and based on a 'topic exchange'

		Constructor args:
		host:        where the queue server is running
		exchange:    exchange to communicate with the other filters
	"""

	def __init__( self, host, exchange, listen_to=None, send_to=None ):
		self.host = host
		self.exchange = exchange
		if listen_to != None:
			self.listen_to = listen_to
		if send_to != None:
			self.send_to = send_to

		print( " [x] Created filter on %r for %r" % ( self.host, self.exchange ))


	def send( self, message, selector=None ):
		'''
			Send a message to some other filter listening on the exchange for the selector
		'''
		if selector == None:
			selector = self.send_to
		self.__send_msg( self.host, self.exchange, selector, message );


	def listen( self, callback, selectors=None ):
		"""
			Listen on the exchange for messages matching the selectors,
			invoke the callback on the messages
		"""

		if selectors == None:
			selectors = self.listen_to
		self.__listen_in( self.host, self.exchange, selectors, callback )


	def __send_msg( self, host, exchange, selector, message ):
		'''
			Send a message to an exchange on a host using a given selector
		'''
		connection = pika.BlockingConnection( pika.ConnectionParameters( host ))
		channel = connection.channel()
		channel.exchange_declare( exchange, exchange_type='topic' )
		channel.basic_publish(    exchange, selector, message )
		connection.close()

	def __listen_in( delf, host, exchange, selectors, callback ):
		"""
			Listen on the exchange for messages matching the selectors,
			invoke the callback on the messages
		"""
		connection = pika.BlockingConnection(pika.ConnectionParameters( host=host ))
		channel = connection.channel()
		channel.exchange_declare( exchange, exchange_type='topic' )
		result = channel.queue_declare( exclusive=True )
		queue_name = result.method.queue   		# Something like "amq.gen-WmsFXfVkqeCbNxvOnw9iqA"

		for selector in selectors:
			channel.queue_bind( exchange=exchange, queue=queue_name, routing_key=selector )
			print(" [x] bound: %s:%s:%s" % (exchange, queue_name, selector))

		channel.basic_consume( callback, queue=queue_name, no_ack=True )
		channel.start_consuming()
	

class Executor():
	"""
		Implements an RPC server executing a service.
	"""

	# Standard constructor
	def __init__( self, host, queue, service ):
		"""
		host: where the message queue is running
		queue: the queue where requests will be accepted
		service: the service to run, should be a function of a single arg

		See ExecutorClient.call()
		"""
		self.host = host
		self.queue = queue
		self.service = service

	# Invoked when a request arrives: call the service with the
	# request's body, then publish the response back to the original
	# caller using the request's reply_to and correlation_id properties
	def __on_execution_request( self, ch, method, props, body ):
		body = body.decode()
		print( " [*] calling %r on %r (pid=%s)" % (self.service, body, str(os.getpid())) )
		response = self.service( body )
		print( " [*] response: %s" % response )
		ch.basic_publish(exchange='',
	                     routing_key=props.reply_to,
	                     properties=pika.BasicProperties(correlation_id = props.correlation_id),
	                     body=str(response))
		ch.basic_ack(delivery_tag = method.delivery_tag)

	# Run the executor: listen for requests on the queue
	def run( self ):
		connection = pika.BlockingConnection(pika.ConnectionParameters( host=self.host ))
		channel = connection.channel()
		channel.queue_declare( self.queue )
		channel.basic_consume( self.__on_execution_request, queue=self.queue )
		channel.start_consuming()


class ExecutorClient(object):
	"""
		Implements the client of RPC server, requesting the execution of
		a service.
	"""

	def __init__(self, host, queue ):
		"""
			host: where the message queue is running
			queue: the queue where requests will be accepted
		"""
		self.host = host
		self.queue = queue
		self.connection = pika.BlockingConnection(pika.ConnectionParameters( host ))
		self.channel = self.connection.channel()
		declaration = self.channel.queue_declare(exclusive=False) # Allow multiple executors to share the same queue
																  # so they can share the load	 
		self.callback_queue = declaration.method.queue
		self.channel.basic_consume( self.__on_response,
									no_ack=True,
									queue=self.callback_queue)


	# Discard any replies that don't match the correlation ID of our
	# last service invocation; otherwise terminate the wait
	def __on_response(self, ch, method, props, body):
		if self.corr_id == props.correlation_id:
			self.response = body

	def call(self, arg):
		"""
			Invoke the remote service passing the arg, then wait 
			for a response
		"""
		self.response = None
		self.corr_id = str(uuid.uuid4())	# Generate a random correlation ID
		props = pika.BasicProperties( reply_to = self.callback_queue,
									  correlation_id = self.corr_id )
		self.channel.basic_publish(
				exchange='',
				routing_key=self.queue,
				properties=props,
				body=arg)

		# Actively waiting for something to come back: let's
		# insert a short sleep to avoid using the CPU at 100%
		while self.response is None:
			time.sleep( 0.1 )
			self.connection.process_data_events()

		return self.response.decode()	# Return a proper string, not bytes
