import requests
import json
import urllib

class DbConnection():
	'''
		A simple API for CounchDB
	'''
	def __init__( self, url ):
		self.url = url

	def __encodeEntityID( self, entityID ):
		return entityID.replace( ":", "_" ).replace( "/", "_" )

	def save( self, database, documentID, document ):
		'''
			Add a new document to a database, or update an existing one
		'''
		docID = self.__encodeEntityID( documentID )

		# See if we have that doc already: if so, 
		(ret,existingDoc) = self.findOne( database, docID )
		if ret != 404:
			# YES, a version of our doc exists already: we need to
			# add the current revision to our doc's fields
			# to allow an update
			revision = existingDoc['_rev']
			# print( ">>> revision:", revision )
			document['_rev'] = revision

		data = json.dumps( document )
		url = "%s/%s/%s" % (self.url, database, docID)
		ret = requests.put( url, data )
		# print( "save(): %s, %s: %s" % (ret.url, ret.status_code, ret.text ))
		return ret.status_code, ret.text

	def findOne( self, database, documentID ):
		''' 
			Find a document by its ID
			Return a status code and possibly the document itself
		'''
		url = "%s/%s/%s" % (self.url, database, self.__encodeEntityID( documentID ))
		ret = requests.get( url )
		# print( "findOne(): %s, %s: %s" % (ret.url, ret.status_code, ret.text ))
		return ret.status_code, json.loads( ret.text )

	def find( self, database, selector ):
		'''
			Search for documents; selector includes the query parameters.
			For instance
			{
				"selector": {
					"state": "PartiallyObserved"
				}
			}
			Returns a possibly empty array of documents
			See http://docs.couchdb.org/en/2.1.1/api/database/find.html
		'''
		queryURL = "%s/%s/_find" % (self.url, database)
		headers = {'Content-Type': 'application/json'}
		ret = requests.post( queryURL, headers=headers, data=json.dumps(selector) )
		# print( "find(): %s, %s: %s" % (ret.url, ret.status_code, ret.text ))
		if( ret.status_code != 200 ):
			raise RuntimeError( "Error: " + str( ret.status_code ) + ": " + ret.text )
		return ret.status_code, json.loads( ret.text )['docs']

	def findAll( self, database ):
		queryURL = "%s/%s/_all_docs" % (self.url, database)
		headers = {'Accept': 'application/json'}
		ret = requests.get( queryURL, headers=headers )
		# print( "find(): %s, %s: %s" % (ret.url, ret.status_code, ret.text ))
		return ret.status_code, json.loads( ret.text )['rows']

	def delete( self, database, documentID, documentRev ):
		''' Delete a document by its ID '''
		url = "%s/%s/%s" % (self.url, database, self.__encodeEntityID( documentID ))
		params = { "rev": documentRev }
		ret = requests.delete( url, params=params )
		# print( "delete(): %s, %s: %s" % (ret.url, ret.status_code, ret.text ))
		return ret.status_code, ret.text

