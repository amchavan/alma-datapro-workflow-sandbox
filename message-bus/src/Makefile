all: compile
	@echo "All done"

test: gen
	python3 test/python/tests.py

gen: compile
	python3 main/python/draws/messages/GenMessage.py -x ../config/Msg2Py.xslt -o TestMsg.py -i TestMsg.xml -d test/python

compile:
	@cd ..; mvn clean install; cd -
	@pip3 install --user --upgrade main/python
