define \n


endef

clean:
	@rm -rf main/*/alma/icd/adapt/messages/generated

all: gen compile
	@echo "All done"

test: tgen
	python3 test/python/tests.py

gen: compile $(wildcard main/resources/*.xml)
	$(foreach res,$(wildcard main/resources/*.xml),python3 -m adapt.messagebus.GenMessage --java -i $(res) -o $(subst .xml,.java,$(subst main/resources/,,$(res))) -d main/java$(\n)	)
	$(foreach res,$(wildcard main/resources/*.xml),python3 -m adapt.messagebus.GenMessage --py -i $(res) -o $(subst .xml,.py,$(subst main/resources/,,$(res))) -d main/python$(\n)	)

tgen:
	python3 -m adapt.messagebus.GenMessage --py   -o TestMsg.py   -i test/resources/TestMsg.xml -d main/python
	python3 -m adapt.messagebus.GenMessage --java -o TestMsg.java -i test/resources/TestMsg.xml -d main/java

compile:
	@cd ..; mvn clean install; cd -
	@pip3 install --user --upgrade main/python
