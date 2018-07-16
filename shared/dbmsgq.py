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
    return datetime.datetime.now().isoformat()#.utcnow().strftime( "%Y-%m-%dT%H-%M-%S.%f" )

class MessageQueue():
	"""
		Implements a RabbitMQ-like message queue.
		Implementation is based on CouchDB

		Constructor args:
		host:     	where the queue server is running
		queueName:	name of queue to communicate on
	"""
	try:
		# This may or may not work -- it's some third party service I found somwewhere
		# It will fail if queried too often, like more than once per second
		myIP = urllib.request.urlopen('http://api.infoip.io/ip').read().decode('utf8')	
	except Exception:
		myIP = "0.0.0.0"

	def __init__( self, host, queueName, listenTo=None, sendTo=None ):

		self.host = host
		self.queueName = queueName
		self.listenTo = None
		self.sendTo = None

		if listenTo != None:
			self.listenTo = listenTo
		if sendTo != None:
			self.sendTo = sendTo
		

		self.dbcon = DbConnection( baseUrl )
		print( " [x] Created queue %s on %s" % ( self.queueName, self.host ))


	def send( self, messageBody, selector=None, addMsgbackID=False ):
		'''
			Send a message to some other filter listening on the queue for the selector
		'''
		if selector == None:
			selector = self.sendTo
		if selector == None:
			raise RuntimeError( "No selectors to send to" )

		now = nowISO()
		
		message = {}
		msgbackID = str( uuid.uuid4() ).replace( "-", "" )
		message['creationTimestamp'] = now
		message['originIP'] = MessageQueue.myIP
		message['selector'] = selector
		message['consumed'] = False
		if addMsgbackID:
			message['msgbackID'] = msgbackID
		message['body'] = messageBody
		messageID = now + "-" + msgbackID	
		print( ">>> Saving at:", message['creationTimestamp'], "(was)", now )
		retcode,msg = self.dbcon.save( self.queueName, messageID, message )
		if retcode != 201:
			raise RuntimeError( "Msg send failed: DB error: %s: %s" % (retcode,msg) )
		return message


	def getNext( self, selector, consume=True, fullMessage=False ):
		"""
			Listen on the queue for messages matching the selectors,
			invoke the callback on the messages
		"""
		
		messages = []
		callTime = time.time()
		# print( ">>> callTime: " + str(callTime) )
		selector = { 
			"selector": 
				{ "$and": [
					{ "selector": { "$regex" : selector }},
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
			retcode,messages = self.dbcon.find( self.queueName, selector )
			# print( ">>> selector:", selector, "found:", len(messages))
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
			self.dbcon.save( self.queueName, ret["_id"], ret )

		print( ">>> found: ", ret )
		if fullMessage:
			return ret
		return ret['body']

	
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


	def listen( self, callback, selector=None, consume=True, fullMessage=False ):
		"""
			Listen on the queueName for messages matching the selector,
			invoke the callback on the messages
		"""

		if selector == None:
			selector = self.listenTo
		if selector == None:
			raise RuntimeError( "No selectors to listen to" )

		while True:
			print( ">>> waiting for message on queue '%s' matching selector '%s' ..." % (self.queueName, selector))
			message = self.getNext( selector, consume, fullMessage=fullMessage )
			print( ">>> got", message )
			callback( message )


class Executor():
	"""
		Implements an RPC server executing a service.
	"""

	# Standard constructor
	def __init__( self, host, queueName, selector, service ):
		"""
		host: where the message queue is running
		queueName: the queueName where requests will be accepted
		service: the service to run, should be a function of a single arg -- a message

		See ExecutorClient.call()
		"""
		self.service = service
		self.selector = selector
		self.queue = MessageQueue( host, queueName, listenTo=selector )

	# Invoked when a request arrives: call the service with the
	# request's body, then publish the response back to the original
	# caller using the incoming message's msgbackID as selector
	def __on_execution_request( self, message ):
		print( ">>> message", message )
		body = message['body']
		print( " [*] calling %r on %r (pid=%s)" % (self.service, body, str(os.getpid())) )
		response = self.service( body )
		print( " [*] response: %s" % response )
		self.queue.send( response, message['msgbackID'])


	# Run the executor: listen for requests on the queue
	def run( self ):
		self.queue.listen( self.__on_execution_request, self.selector, fullMessage=True )


class ExecutorClient():
	"""
		Implements the client of RPC server, requesting the execution of
		a service.
	"""

	def __init__( self, host, queueName, selector ):
		"""
			host: where the message queue is running
			queueName: the queueName where requests will be accepted
		"""
		self.selector = selector
		self.queue = MessageQueue( host, queueName, sendTo=selector )


	def call( self, body ):
		"""
			Invoke the remote service passing the message body, then wait 
			for a response on the msgbackID selector
		"""
		message = self.queue.send( body, addMsgbackID=True )
		msgbackID = message['msgbackID']
		ret = self.queue.getNext( msgbackID )
		return ret

