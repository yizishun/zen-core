SIM ?= verilator
TOPLEVEL_LANG ?= verilog

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

.PHONY: sim-python-iverilog sim-python-verilator sim-python-vcs
sim-python-iverilog: verilog
	$(eval SIM=icarus)
	$(eval SIM_BUILD = $(IVERILOG_DIR))
	poetry run \
		make -C $(PY_TB_DIR) \
			-f $(COCOTB_MAKEFILE)
	mv $(IVERILOG_DIR)/$(DESIGN).fst $(BUILD_DIR)/wave.fst

sim-python-verilator: verilog
	$(eval SIM=verilator)
	$(eval SIM_BUILD = $(VERILATOR_DIR))
	$(eval EXTRA_ARGS += -CFLAGS -std=c++20)
	$(eval EXTRA_ARGS += --trace --trace-structs)
	poetry run \
		make -C $(PY_TB_DIR) \
			-f $(COCOTB_MAKEFILE)
	mv $(PY_TB_DIR)/dump.vcd $(BUILD_DIR)/wave.vcd

#dont use,i dont have test environment
sim-python-vcs:
	$(eval SIM=vcs)
	poetry run \
		make -C $(TB_DIR)/python-tb \
			-f $(COCOTB_MAKEFILE) \


