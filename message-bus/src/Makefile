all: compile
	@echo "All done"

test: gen
	python3 test/python/tests.py

gen: compile
	python3 -m main.python.draws.messages.GenMessage --py -o TestMsg.py -i test/resources/TestMsg.xml -d test/python
	python3 -m main.python.draws.messages.GenMessage --java -o TestMsg.java -i test/resources/TestMsg.xml -d test/java

compile:
	@cd ..; mvn clean install; cd -
	@pip3 install --user --upgrade main/python
