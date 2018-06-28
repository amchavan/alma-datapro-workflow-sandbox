import urllib.request
import json

baseUrl = "http://localhost:3000"
resp = urllib.request.urlopen( baseUrl + "/posts" ).read().decode( 'utf-8' )
cont = json.loads( resp )
print( cont )

## Parse JSON
counter=0
for item in cont:
    counter += 1
    print( counter, "Title:", item['title'], " Author:", item['author'])

##print formated
#print (json.dumps(cont, indent=4, sort_keys=True))
print("Number of titles: ", counter)


'''
import http.client
import json

connection = http.client.HTTPSConnection('api.github.com')

headers = {'Content-type': 'application/json'}

foo = {'text': 'Hello world github/linguist#1 **cool**, and #1!'}
json_foo = json.dumps(foo)

connection.request('POST', '/markdown', json_foo, headers)

response = connection.getresponse()
print(response.read().decode())
'''