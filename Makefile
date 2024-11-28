TOP = TB
BUILD_DIR = ./build
SCRIPTS_DIR = ./scripts
TB_DIR = ./tb

VSRCS = $(shell find $(abspath $(BUILD_DIR)) -name "*.v" -or -name "*.sv")
TB_SRCS = $(shell find $(abspath $(SV_TB_DIR)) -name "*.v" -or -name "*.sv" -or -name "*.cpp" -or -name "*.cc" -or -name "*.c")

SV_TB_DIR = $(TB_DIR)/sv-tb
VCD_FILE = $(BUILD_DIR)/wave.vcd

include $(SCRIPTS_DIR)/elaborate.mk

include $(SCRIPTS_DIR)/sim-sv-verilator.mk

include $(SCRIPTS_DIR)/sim-sv-vcs.mk

vcd: 
	gtkwave $(VCD_FILE) &

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
