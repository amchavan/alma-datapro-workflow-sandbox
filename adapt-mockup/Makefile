MODULES:=icd aqua-batch-helper aqua-qa2 data-tracker dra draws-dashboard pipeline pldriver product-ingestor xtss

#1: Module name
define makeModule
.PHONY: $1
$1:
	@$(if $(wildcard $1/src/Makefile),make -C $1/src $(MAKECMDGOALS),echo "No Makefile in module $1!")
endef

$(foreach mod,$(MODULES),$(eval $(call makeModule,$(mod))))

all: $(MODULES)
	@echo "All done"
