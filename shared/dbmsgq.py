import uuid
import time
import os
import socket
import uuid
import urllib.request
import datetime

import dbdrwutils
from dbcon import DbConnection


# Utility classes and functions for dealing with a message queue -- the implementation
# is specific to CouchDB
#
# A M Chavan, 13-Jul-2018

baseUrl = "http://localhost:5984" # CouchDB

def nowISO():
    return datetime.datetime.now().isoformat()[:-3]

class MqConnection():
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
		# print( " [x] Created queue %s on %s" % ( self.queueName, self.host ))

	def send( self, messageBody, selector=None, addMsgbackID=False ):
		'''
			Send a message to some other filter listening on the queue for the selector
		'''
		if selector == None:
			selector = self.sendTo
		if selector == None:
			raise RuntimeError( "No selectors to send to" )

		now = nowISO()

		# Are we breadcasting to a group?
		if not selector.endswith( '.*' ):
			# NO, just to a single receiver
			return self._send( now, selector, addMsgbackID, messageBody )
		else:
			# YES, let's retrieve the group and send to all its participants
			# TODO: cache group definition somewhere instead of querying
			#       the database every time
			retcode,group = self.dbcon.findOne( self.queueName, selector )
			if retcode == 404:
				raise RuntimeError( "Group not found: %" % selector )
			messages = []
			for member in group['members']:
				m = self._send( now, member, addMsgbackID, messageBody )
				messages.append( m )
			return messages

	def _send(self, now, selector, addMsgbackID, messageBody):
		message = {}
		msgbackID = str( uuid.uuid4() ).replace( "-", "" )
		message['creationTimestamp'] = now
		message['originIP'] = MqConnection.myIP
		message['selector'] = selector
		message['consumed'] = False
		if addMsgbackID:
				message['msgbackID'] = msgbackID
		message['body'] = messageBody
		messageID = now + "-" + msgbackID
		retcode,msg = self.dbcon.save( self.queueName, messageID, message )
		if retcode != 201:
				raise RuntimeError( "Msg send failed: DB error: %s: %s" % (retcode,msg) )
		return message

	def getNext( self, selector, consume=True, fullMessage=False, condition=None ):
		"""
			Listen on the queue for for new messages, return the oldest we find.
			Args:
			selector: defines what messages to listen to
			consume:  if True, the message will be marked as consumed and no other
			          listener will receive  (default is True)
          	fullMessage: if True, the message's metadata will be passed in as well and
          	             the actual message will be in the 'body' field (default False)
			condition: boolean function to be invoked before starting to listen, will cause the
			           thread to sleep if the condition is false
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
			# requires an index to be created and I don't care about
			# that right now -- amchavan, 13-Jul-2018
			#
			# TODO: revisit this if needed
			}

		# See if we can even start listening: if we have a conditional expression
		# and it evaluates to False we need to wait a bit
		while ( condition and (condition() == False) ):
			time.sleep( dbdrwutils.incrementalSleep( callTime ))

		while True:
			retcode,messages = self.dbcon.find( self.queueName, selector )
			# print( ">>> selector:", selector, "found:", len(messages))
			if retcode == 200:
				if len( messages ) != 0:
					break
				else:
					time.sleep( dbdrwutils.incrementalSleep( callTime ))
			else:
				raise RuntimeError( "Msg read failed: DB error: %s: %s" % (retcode,messages) )

		# print( ">>> found: ", messages )
		# print( ">>> found: ", len( messages ))
		messages.sort( key=lambda x: x['creationTimestamp'] )	# Oldest first
		ret = messages[0]
		if consume:
			ret['consumed'] = True
			self.dbcon.save( self.queueName, ret["_id"], ret )

		# print( ">>> found: ", ret )
		if fullMessage:
			return ret
		return ret['body']

	def listen( self, callback, selector=None, consume=True, fullMessage=False, condition=None ):
		"""
			Listen on the queueName for messages matching the selector and process them.
			Args:
			callback: function to process the message with
			selector: defines what messages to listen to
			consume:  UNUSED: if True, the message will be marked as consumed and no other
			          listener will receive  (default is True)
          	fullMessage: UNUSED: if True, the message's metadata will be passed in as well and
          	             the actual message will be in the 'body' field (default False)
			condition: boolean function to be invoked before starting to listen, will cause the
			           thread to sleep if the condition is false
		"""

		if selector == None:
			selector = self.listenTo
		if selector == None:
			raise RuntimeError( "No selectors to listen to" )

		while True:
			# print( ">>> waiting for message on queue '%s' matching selector '%s' ..." % (self.queueName, selector))
			message = self.getNext( selector, consume, fullMessage=fullMessage, condition=condition )
			# print( ">>> got", message )
			callback( message )
	
	def joinGroup( self, groupName, listener=None ):
		"""
			Join a group as a listener. Messages sent to the group will be
			passed on to this instance as well. 

			Group names must end with '.*'

			Arg listener defaults to the value of the listenTo
			constructor arg.
		"""
		if groupName == None:
			raise RuntimeError( "No group name to join" )
		if not groupName.endswith( ".*" ):
			raise RuntimeError( "Group names must end with .*" )

		if listener == None:
			listener = self.listenTo
		if listener == None:
			raise RuntimeError( "No listener to join as" )

		# We want to join the group groupName as listener
		# First let's see if that group exists, otherwise we'll create it
		retcode,group = self.dbcon.findOne( self.queueName, groupName )
		if retcode == 404:
			print( ">>> not found: group=%s" % groupName )
			group = {}
			# group['groupName'] = groupName
			group['members'] = [listener]
			self.dbcon.save( self.queueName, groupName, group )
			print( ">>> created: group=%s" % groupName )
			return

		# Found a group with that name: if needed, add ourselves to it
		print( ">>> found: group=%s" % group )
		members = group['members']
		if not listener in members:
			group['members'].append( listener )
			self.dbcon.save( self.queueName, groupName, group )
			print( ">>> added ourselves as members: group=%s" % group )
		else:
			print( ">>> already in members list: listener=%s" % listener )
		

		





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
		self.queue = MqConnection( host, queueName, listenTo=selector )

	# Invoked when a request arrives: call the service with the
	# request's body, then publish the response back to the original
	# caller using the incoming message's msgbackID as selector
	def __on_execution_request( self, message ):
		# print( ">>> message", message )
		body = message['body']
		# print( " [*] calling %r on %r (pid=%s)" % (self.service, body, str(os.getpid())) )
		response = self.service( body )
		# print( " [*] response: %s" % response )
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
		self.queue = MqConnection( host, queueName, sendTo=selector )


	def call( self, body ):
		"""
			Invoke the remote service passing the message body, then wait 
			for a response on the msgbackID selector
		"""
		message = self.queue.send( body, addMsgbackID=True )
		msgbackID = message['msgbackID']
		ret = self.queue.getNext( msgbackID )
		return ret

