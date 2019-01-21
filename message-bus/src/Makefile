all: compile
	@echo "All done"

test: compile
	python3 test/python/tests.py

compile:
	@cd ..; mvn clean install; cd -
	@pip3 install --user --upgrade main/python
