py3:=$(if $(filter $(PYVER),3),3,)
all: compile
	@echo "All done"

test: compile
	python$(py3) test/python/tests.py

compile:
	@cd ..; mvn clean install; cd -
	pip$(py3) install --user --upgrade main/python
