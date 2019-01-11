MODULES:=demos-common basic-receiver basic-sender

#1: Module name
define makeModule
.PHONY: $1
$1:
	@$(if $(wildcard $1/src/Makefile),make -C $1/src $(MAKECMDGOALS),echo "No Makefile in module $1!")
endef

$(foreach mod,$(MODULES),$(eval $(call makeModule,$(mod))))

all: $(MODULES)
	@mvn clean package
	@echo "All done"

