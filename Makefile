TOP := TB
#common DIR
BUILD_DIR = ./build
SCRIPTS_DIR = ./scripts
TB_DIR = ./tb-$(DESIGN)

#common files
VSRCS = $(shell find $(abspath $(BUILD_DIR)) -name "*.v" -or -name "*.sv")
VCD_FILE = $(BUILD_DIR)/wave.vcd

#------------------language specific --------------
# three type variables
# 1, {language name}_TB_DIR : directory of tb
# 2, {language name}_TB_SRCS : tb source files
# 3, {language name}_TB_INC : include path
#uvm tb specific
UVM_TB_DIR = $(TB_DIR)/uvm-tb
UVM_TB_SRCS = $(shell find $(abspath $(UVM_TB_DIR)) -name "*.v" -or -name "*.sv" -or -name "*.cpp" -or -name "*.cc" -or -name "*.c")
UVM_TB_INC = +incdir+$(abspath $(UVM_TB_DIR))

#sv tb specific
SV_TB_DIR = $(TB_DIR)/sv-tb
SV_TB_SRCS = $(shell find $(abspath $(SV_TB_DIR)) -name "*.v" -or -name "*.sv" -or -name "*.cpp" -or -name "*.cc" -or -name "*.c")
SV_TB_INC = +incdir+$(abspath $(SV_TB_DIR))

#cpp tb specific
CPP_TB_DIR = $(TB_DIR)/cpp-tb
CPP_TB_SRCS = $(shell find $(abspath $(CPP_TB_DIR)) -name "*.cpp" -or -name "*.cc" -or -name "*.c")
CPP_TB_INC = -I$(CPP_TB_DIR)/include

#-------------------simulator specific--------------
# three type variables
# 1, {simulator name}_DIR : simulator directory
# 2, {simulator name}_FLAGS : simulator flags
# 3, {simulator name}_BIN : simulator binary
# verilator specific
VERILATOR_DIR = $(BUILD_DIR)/verilator
VERILATOR_FLAGS = --trace --timing --threads 8 -O1 --build --exe -cc --top $(TOP) -Mdir $(VERILATOR_DIR)
VERILATOR_BIN = $(VERILATOR_DIR)/V$(TOP)

# vcs specific
VCS_DIR = $(BUILD_DIR)/vcs
VCS_FLAGS = -full64 -timescale=1ns/1ns -debug_access+all -l $(VCS_DIR)/vcs.log -o $(VCS_BIN) -sverilog -Mdir=$(VCS_OBJDIR)
VCS_OBJDIR = $(VCS_DIR)/obj
VCS_BIN = $(VCS_DIR)/simv

#--------------------disgn specific------------------
DESIGN := GCD #can be overrided in command line

# include scripts
include $(SCRIPTS_DIR)/elaborate.mk

include $(SCRIPTS_DIR)/sim-sv-verilator.mk

include $(SCRIPTS_DIR)/sim-sv-vcs.mk

include $(SCRIPTS_DIR)/sim-cpp-verilator.mk

include $(SCRIPTS_DIR)/sim-uvm-vcs.mk

vcd: 
	gtkwave $(VCD_FILE) &


sim-chisel-verilator:
sim-chisel-vcs:
sim-python-verilator:
sim-python-vcs:
-include ../Makefile
