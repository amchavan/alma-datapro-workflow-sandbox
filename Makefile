MODULES:=message-bus message-bus-demos draws-mockup

#1: Module name
define makeModule
.PHONY: $1
$1:
	@$(if $(wildcard $1/Makefile),make -C $1/ $(MAKECMDGOALS),$(if $(wildcard $1/src/Makefile),make -C $1/src $(MAKECMDGOALS),echo "No Makefile in module $1!"))
endef

$(foreach mod,$(MODULES),$(eval $(call makeModule,$(mod))))

all: $(MODULES)
	@echo "All done"
