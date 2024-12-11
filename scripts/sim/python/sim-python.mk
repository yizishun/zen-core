TB_SRCS = $(shell find $(abspath $(TB_DIR) -name "*.py"))
TB_INC =

SIM ?= verilator
TOPLEVEL_LANG ?= verilog

VSRCS = $(shell find $(abspath $(RTL_DIR)) -maxdepth 1 -name "*.v" -or -name "*.sv")
VSRCS += $(foreach layers, $(LAYERS.$(SIM)), $(call collect_layer, $(RTL_DIR)/$(layers)))
VERILOG_SOURCES += $(VSRCS)

# TOPLEVEL is the name of the toplevel module in your Verilog or VHDL file
TOPLEVEL = $(DESIGN)

# MODULE is the basename of the Python test file
MODULE = tb

EXTRA_ARGS =

SIM_BUILD =

WAVES = 1

COCOTB_MAKEFILE = $(shell poetry run cocotb-config --makefiles)/Makefile.sim

export SIM TOPLEVEL TOPLEVEL_LANG VERILOG_SOURCES MODULE EXTRA_ARGS SIM_BUILD WAVES
