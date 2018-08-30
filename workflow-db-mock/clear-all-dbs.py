#!/usr/bin/env python3.6

import json
import requests

#############################################################################
## 
## WARNING: very distructive!
## Deletes all documents in all user databases
## From https://moduliertersingvogel.de/2018/03/15/bulk-delete-in-couchdb/
##
#############################################################################

r = requests.get( "http://localhost:5984/_all_dbs" )
# print( r )
allDatabases=json.loads(r.text)
userDatabases = []
for db in allDatabases:
    if not db.startswith( "_" ):
        userDatabases.append( db )
        
print( userDatabases )

for db in userDatabases:
    print( db )
    url = "http://localhost:5984/%s/_all_docs" % db
    r=requests.get( url )
    docs=json.loads(r.text)['rows']
    print( docs )
    todelete=[]
    for doc in docs:
        todelete.append({"_deleted": True, "_id": doc["id"], "_rev": doc["value"]["rev"]})
        url = "http://localhost:5984/%s/_bulk_docs" % db
        r=requests.post( url, json={"docs": todelete})
        if r.status_code != 201:
            print( "Error %s: %s %s" % (r.status_code, url, todelete ))