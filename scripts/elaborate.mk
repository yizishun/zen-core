
RTL_DIR = $(BUILD_DIR)/rtl
ELABORATE_DIR = $(RTL_DIR)/elaborate
CONFIG_DIR = ./config
FIR_FILES = $(shell find $(abspath $(ELABORATE_DIR)) -name "*.fir")
SV_FILES := $(foreach file,$(FIR_FILES),$(RTL_DIR)/$(basename $(notdir $(file))).sv)
FIRTOOL_OPTION = --lowering-options=disallowLocalVariables,disallowPackedArrays,locationInfoStyle=wrapInAtSquareBracket

config:
	mkdir -p $(CONFIG_DIR)
	mill -i elaborateRTL.runMain elaborate.Elaborate_$(DESIGN) config --width 32 --useAsyncReset true --target-dir config

fir: config
	mkdir -p $(ELABORATE_DIR)
	mill -i elaborateRTL.runMain elaborate.Elaborate_$(DESIGN) design --target-dir $(ELABORATE_DIR) --parameter ./config/$(DESIGN).json

verilog: fir
	mkdir -p $(RTL_DIR)
	for file in $(FIR_FILES); do \
		firtool $(FIRTOOL_OPTION) $$file -o $(RTL_DIR)/$$(basename $$file .fir).sv; \
	done

help:
	mill -i elaborateRTL.run --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

bsp:
	mill -i mill.bsp.BSP/install

idea:
	mill -i mill.idea.GenIdea/idea

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: test verilog help reformat checkformat clean