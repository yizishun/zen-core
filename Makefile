BUILD_DIR = ./build
ELABORATE_DIR = $(BUILD_DIR)/elaborate
CONFIG_DIR = ./config
TOP = TB

config:
	mkdir -p $(CONFIG_DIR)
	mill -i elaborateRTL.run config --width 32 --useAsyncReset true --target-dir config

fir: config
	mkdir -p $(ELABORATE_DIR)
	mill -i elaborateRTL.run design --target-dir $(ELABORATE_DIR) --parameter ./config/GCD.json


verilog: fir
	firtool $(ELABORATE_DIR)/GCD.fir -o $(BUILD_DIR)/GCD.v


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
VERILATOR_FLAGS = --trace --timing --threads 8 -O1 --main --build --exe -cc --top $(TOP)
VSRCS = $(shell find $(abspath $(BUILD_DIR)) -name "*.v" -or -name "*.sv")
TB_DIR = ./tb
SV_TB_DIR = $(TB_DIR)/sv-tb
TB_SRCS = $(shell find $(abspath $(SV_TB_DIR)) -name "*.v" -or -name "*.sv" -or -name "*.cpp")
OBJ_DIR = $(BUILD_DIR)/obj
BIN = $(BUILD_DIR)/$(TOPNAME)
sim-sv-verilator: $(TB_SRCS) $(VSRCS)
	$(call git_commit, "sim RTL")
	verilator $(VERILATOR_FLAGS) \
		-Mdir $(OBJ_DIR) \
		$^

sim-sv-vcs:
sim-sv-iverilog:
sim-uvm-verilator:
sim-uvm-vcs:
sim-uvm-iverilog:
sim-cpp-verilator:
sim-chisel-verilator:
sim-chisel-vcs:
sim-chisel-iverilog:
sim-python-verilator:
sim-python-vcs:
sim-python-iverilog:
-include ../Makefile
