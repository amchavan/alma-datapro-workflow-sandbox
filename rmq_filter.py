import pika
import argparse

class Filter():
	"""
		Implements a filter in the 'Pipes and filters' pattern: see
			https://docs.microsoft.com/en-us/azure/architecture/patterns/pipes-and-filters
		Implementation is specific to RabbitMQ (the pipe) and based on a 'topic exchange'

		Constructor args:
		host:        where the queue server is running
		exchange:    exchange to communicate with the other filters
		filter_type: can be either SEND_ONLY, RECEIVE_ONLY, SEND_RECEIVE, CMD_REPLY
	"""

	# All our filter types
	filter_types = ["SEND_ONLY", "RECEIVE_ONLY", "SEND_RECEIVE", "CMD_REPLY"]

	# Filter types requiring listen selectors
	must_listen_filter_types = [ "RECEIVE_ONLY", "SEND_RECEIVE", "CMD_REPLY" ]

	# Filter types requiring send selectors
	must_send_filter_types =   [ "SEND_ONLY", "SEND_RECEIVE" ]

	def __init__( self, host, exchange, filter_type=None, listen_to=None, send_to=None ):
		if filter_type not in Filter.filter_types:
			raise ValueError( 'Invalid filter type: %r' % filter_type )
		self.host = host
		self.exchange = exchange
		self.type = filter_type
		if listen_to != None:
			self.listen_to = listen_to
		if send_to != None:
			self.send_to = send_to
		print( " [x] Created %r filter on %r for %r" % ( self.type, self.host, self.exchange ))


	def send( self, message, selector=None ):
		'''
			Send a message to some other filter listening on the exchange for the selector
		'''
		if selector == None:
			selector = self.send_to
		Filter.send_msg( self.host, self.exchange, selector, message );

	def listen( self, callback, selectors=None ):
		"""
			Listen on the exchange for messages matching the selectors,
			invoke the callback on the messages
		"""

		if selectors == None:
			selectors = self.listen_to
		Filter.listen_in( self.host, self.exchange, selectors, callback )

	@staticmethod
	def send_msg( host, exchange, selector, message ):
		'''
			Send a message to an exchange on a host using a given selector
		'''
		connection = pika.BlockingConnection( pika.ConnectionParameters( host ))
		channel = connection.channel()
		channel.exchange_declare( exchange, exchange_type='topic' )
		channel.basic_publish(    exchange, selector, message )
		connection.close()

	@staticmethod
	def listen_in( host, exchange, selectors, callback ):
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

	@staticmethod
	def arg_parser( description, filter_type ):
		""" Basic set up of a command line argument parser """
		parser = argparse.ArgumentParser( description=description )
		listen_helptext = "A comma-separated list of selectors for incoming messages"
		send_helptext = "Selector for sent messages"
		if filter_type in Filter.must_listen_filter_types:
			parser.add_argument( '-listen', action="store", required=True, help=listen_helptext )
		if filter_type in Filter.must_send_filter_types:
			parser.add_argument( '-send', action="store", required=True, help=send_helptext )
		return parser
