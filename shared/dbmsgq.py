from dbcon import DbConnection
import uuid
import time
import os
import socket
import uuid
import urllib.request
import datetime


# Utility classes and functions for dealing with a message queue -- the implementation
# is specific to CouchDB
#
# A M Chavan, 13-Jul-2018

baseUrl = "http://localhost:5984" # CouchDB

def nowISO():
    return datetime.datetime.utcnow().strftime( "%Y-%m-%dT%H-%M-%S" )

class MessageQueue():
	"""
		Implements a RabbitMQ-like message queue.
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
		self.myIP = urllib.request.urlopen('http://api.infoip.io/ip').read().decode('utf8')	# Hack!! this may not work
		self.dbcon = DbConnection( baseUrl )
		print( " [x] Created filter on %r for %r" % ( self.host, self.exchange ))


	def send( self, message, selector=None ):
		'''
			Send a message to some other filter listening on the exchange for the selector
		'''
		if selector == None:
			selector = self.send_to
		self.__send_msg( selector, message );


	def listen( self, callback, selectors=None ):
		"""
			Listen on the exchange for messages matching the selectors,
			invoke the callback on the messages
		"""

		if selectors == None:
			selectors = self.listen_to
		self.__listen_in( self.host, self.exchange, selectors, callback )


	def __send_msg( self, selector, messageBody ):
		'''
			Send a message to an exchange on a host using a given selector
		'''
		now = nowISO()
		message = {}
		message['creationTimestamp'] = now
		message['originIP'] = self.myIP
		message['selector'] = selector
		message['consumed'] = False
		message['body'] = messageBody
		messageID = now + "-" + str( uuid.uuid4() )

		retcode,msg = self.dbcon.save( self.exchange, messageID, message )
		if retcode != 201:
			raise RuntimeError( "Msg send failed: DB error: %s: %s" % (retcode,msg) )

	def getNext( self, selectors, consume=True ):
		"""
			Listen on the exchange for messages matching the selectors,
			invoke the callback on the messages
		"""
		
		messages = []
		callTime = time.time()
		# print( ">>> callTime: " + str(callTime) )
		selector = { 
			"selector": 
				{ "$and": [
					{ "selector": { "$regex" : selectors }},
					{ "consumed": False }
					]
				} 
			#,
			# "sort": [{"creationTimestamp":"desc"}]
			# 
			# We should let the server sort the results but that
			# requires an index to be created an I don't care about
			# that now -- amchavan, 13-Jul-2018
			#
			# TODO: revisit this if needed
			}

		while True:
			retcode,messages = self.dbcon.find( self.exchange, selector )
			if retcode == 200:
				if len( messages ) != 0:
					break
				else:
					time.sleep( self.__incrementalSleep( callTime ))
			else:
				raise RuntimeError( "Msg read failed: DB error: %s: %s" % (retcode,messages) )

		# print( ">>> found: ", messages )
		# print( ">>> found: ", len( messages ))
		messages.sort( key=lambda x: x['creationTimestamp'] )	# Oldest first
		ret = messages[0]
		if consume:
			ret['consumed'] = True
			self.dbcon.save( self.exchange, ret["_id"], ret )

		print( ">>> found: ", ret )
		return ret
	
	def __incrementalSleep( self, startTime ):
		'''
			Returns the number of seconds to sleep waiting for an event to appear;
			sleep time increases with how long we've been waiting already
		'''
		now = time.time()
		waitingSince = now - startTime
		sleep = -1
		if waitingSince <= 60:
			# waiting since a minute or less: sleep for one sec
			sleep = 1
		elif waitingSince <= 300:
			# waiting since five minutes or less: sleep for 5 sec
			sleep = 5
		elif waitingSince <= 3600:
			# waiting since one hour or less: sleep for 30 sec
			sleep = 30
		else:
			# waiting since a long time: sleep for a minute
			sleep = 60
		# print( ">>> waitingSince: ", waitingSince, ", sleep for: ", sleep )
		return sleep


	def listen( self, callback, selectors=None, consume=True ):
		"""
			Listen on the exchange for messages matching the selectors,
			invoke the callback on the messages
		"""

		if selectors == None:
			selectors = self.listen_to

		while True:
			print( ">>> waiting for message...")
			message = self.getNext( selectors, consume )
			callback( message['body'] )


# class Executor():
# 	"""
# 		Implements an RPC server executing a service.
# 	"""

# 	# Standard constructor
# 	def __init__( self, host, queue, service ):
# 		"""
# 		host: where the message queue is running
# 		queue: the queue where requests will be accepted
# 		service: the service to run, should be a function of a single arg

# 		See ExecutorClient.call()
# 		"""
# 		self.host = host
# 		self.queue = queue
# 		self.service = service

# 	# Invoked when a request arrives: call the service with the
# 	# request's body, then publish the response back to the original
# 	# caller using the request's reply_to and correlation_id properties
# 	def __on_execution_request( self, ch, method, props, body ):
# 		body = body.decode()
# 		print( " [*] calling %r on %r (pid=%s)" % (self.service, body, str(os.getpid())) )
# 		response = self.service( body )
# 		print( " [*] response: %s" % response )
# 		ch.basic_publish(exchange='',
# 	                     routing_key=props.reply_to,
# 	                     properties=pika.BasicProperties(correlation_id = props.correlation_id),
# 	                     body=str(response))
# 		ch.basic_ack(delivery_tag = method.delivery_tag)

# 	# Run the executor: listen for requests on the queue
# 	def run( self ):
# 		connection = pika.BlockingConnection(pika.ConnectionParameters( host=self.host ))
# 		channel = connection.channel()
# 		channel.queue_declare( self.queue )
# 		channel.basic_consume( self.__on_execution_request, queue=self.queue )
# 		channel.start_consuming()


# class ExecutorClient(object):
# 	"""
# 		Implements the client of RPC server, requesting the execution of
# 		a service.
# 	"""

# 	def __init__(self, host, queue ):
# 		"""
# 			host: where the message queue is running
# 			queue: the queue where requests will be accepted
# 		"""
# 		self.host = host
# 		self.queue = queue
# 		self.connection = pika.BlockingConnection(pika.ConnectionParameters( host ))
# 		self.channel = self.connection.channel()
# 		declaration = self.channel.queue_declare(exclusive=False) # Allow multiple executors to share the same queue
# 																  # so they can share the load	 
# 		self.callback_queue = declaration.method.queue
# 		self.channel.basic_consume( self.__on_response,
# 									no_ack=True,
# 									queue=self.callback_queue)


# 	# Discard any replies that don't match the correlation ID of our
# 	# last service invocation; otherwise terminate the wait
# 	def __on_response(self, ch, method, props, body):
# 		if self.corr_id == props.correlation_id:
# 			self.response = body

# 	def call(self, arg):
# 		"""
# 			Invoke the remote service passing the arg, then wait 
# 			for a response
# 		"""
# 		self.response = None
# 		self.corr_id = str(uuid.uuid4())	# Generate a random correlation ID
# 		props = pika.BasicProperties( reply_to = self.callback_queue,
# 									  correlation_id = self.corr_id )
# 		self.channel.basic_publish(
# 				exchange='',
# 				routing_key=self.queue,
# 				properties=props,
# 				body=arg)

# 		# Actively waiting for something to come back: let's
# 		# insert a short sleep to avoid using the CPU at 100%
# 		while self.response is None:
# 			sleep( 0.1 )
# 			self.connection.process_data_events()

# 		return self.response.decode()	# Return a proper string, not bytes
