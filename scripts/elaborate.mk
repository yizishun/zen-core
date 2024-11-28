
RTL_DIR = $(BUILD_DIR)/rtl
ELABORATE_DIR = $(RTL_DIR)/elaborate
CONFIG_DIR = ./config
config:
	mkdir -p $(CONFIG_DIR)
	mill -i elaborateRTL.run config --width 32 --useAsyncReset true --target-dir config

fir: config
	mkdir -p $(ELABORATE_DIR)
	mill -i elaborateRTL.run design --target-dir $(ELABORATE_DIR) --parameter ./config/GCD.json


verilog: fir
	firtool $(ELABORATE_DIR)/GCD.fir -o $(RTL_DIR)/GCD.v


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