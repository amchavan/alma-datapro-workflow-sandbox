clean:
	@rm -rf main/*/alma/icd/adapt/messages/generated

all: gen compile
	@echo "All done"

test: gen
	python3 test/python/tests.py

gen:
	python3 -m main.python.GenMessage --py   -o TestMsg.py   -i test/resources/TestMsg.xml -d main/python
	python3 -m main.python.GenMessage --java -o TestMsg.java -i test/resources/TestMsg.xml -d main/java

compile:
	@cd ..; mvn clean install; cd -
	@pip3 install --user --upgrade main/python
