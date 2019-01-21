#! /bin/bash
for i in $(ls main/resources/*.xml --color=never |sed "s/.*\/\(.*\)\.xml/\1/"); do
	echo python3 -m adapt.messagebus.GenMessage --java -i main/resources/$i.xml -o $i.java -d main/java
	echo python3 -m adapt.messagebus.GenMessage --py -i main/resources/$i.xml -o $i.py -d main/python
done
