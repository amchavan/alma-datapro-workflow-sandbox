#!/usr/bin/env python3

import sys
sys.path.insert(0, "shared")
import dbcon

baseUrl = "http://127.0.0.1:5984" # CouchDB
dbName = "status-entities"

dbcon = dbcon.DbConnection( baseUrl )
doc   = {"entityId":"uid://A001/X1/X1", "state":"None"}
docID = "uid://A001/X1/X1"
dbcon.save( dbName, docID, doc )
(retcode,doc2)  = dbcon.findOne( dbName, docID )
doc2['state'] = "PartiallyObserved"
dbcon.save( dbName, docID, doc2 )

selector = {"selector": {"state": "PartiallyObserved"}}
(retcode,documents) = dbcon.find( dbName, selector )

print( documents )
revision = documents[0]['_rev']
dbcon.delete( dbName, docID, revision )
