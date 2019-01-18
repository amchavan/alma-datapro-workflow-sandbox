all: gen compile
	@echo "All done"

gen: $(wildcard main/resources/*.xml)
	@./gen.sh

compile: gen
	@pip3 install --user --upgrade main/python
