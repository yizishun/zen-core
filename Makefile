TOP := TB
#common DIR
BUILD_DIR = $(abspath ./build)
SCRIPTS_DIR = ./scripts
TB_DIR = ./tb/tb-$(DESIGN)

#common files
VSRCS = $(shell find $(abspath $(RTL_DIR)) -name "*.v" -or -name "*.sv")
VCD_FILE = $(BUILD_DIR)/wave.vcd
FST_FILE = $(BUILD_DIR)/wave.fst
WAVE_VIEWER = gtkwave

#------------------language specific --------------
# three type variables
# 1, {language name}_TB_DIR : directory of tb
# 2, {language name}_TB_SRCS : tb source files
# 3, {language name}_TB_INC : include path
#uvm tb specific
UVM_TB_DIR = $(TB_DIR)/uvm-tb
#UVM_TB_SRCS = $(shell find $(abspath $(UVM_TB_DIR)) -name "*.v" -or -name "*.sv" -or -name "*.cpp" -or -name "*.cc" -or -name "*.c")
UVM_TB_SRCS=$(UVM_TB_DIR)/gcd_pkg.sv $(UVM_TB_DIR)/tb.sv $(UVM_TB_DIR)/transaction/interface.sv $(UVM_TB_DIR)/clib/check.cpp
UVM_TB_INC = +incdir+$(abspath $(UVM_TB_DIR))

#sv tb specific
SV_TB_DIR = $(TB_DIR)/sv-tb
SV_TB_SRCS = $(shell find $(abspath $(SV_TB_DIR)) -name "*.v" -or -name "*.sv" -or -name "*.cpp" -or -name "*.cc" -or -name "*.c")
SV_TB_INC = +incdir+$(abspath $(SV_TB_DIR))

#cpp tb specific
CPP_TB_DIR = $(TB_DIR)/cpp-tb
CPP_TB_SRCS = $(shell find $(abspath $(CPP_TB_DIR)) -name "*.cpp" -or -name "*.cc" -or -name "*.c")
CPP_TB_INC = -I$(CPP_TB_DIR)/include

#python tb specific
PY_TB_DIR = $(TB_DIR)/python-tb
PY_TB_SRCS = $(shell find $(abspath $(PY_TB_DIR) -name "*.py"))
PY_TB_INC =

#-------------------simulator specific--------------
# three type variables
# 1, {simulator name}_DIR : simulator build directory
# 2, {simulator name}_FLAGS : simulator flags
# 3, {simulator name}_BIN : simulator build binary
# verilator specific
VERILATOR_DIR = $(BUILD_DIR)/verilator
VERILATOR_FLAGS = --trace --timing -j 8 -O1 --build --exe -cc --top $(TOP) -Mdir $(VERILATOR_DIR) -CFLAGS -std=c++20
VERILATOR_BIN = $(VERILATOR_DIR)/V$(TOP)

# vcs specific
VCS_DIR = $(BUILD_DIR)/vcs
VCS_FLAGS = -full64 -timescale=1ns/1ns -debug_access+all -l $(VCS_DIR)/vcs.log -o $(VCS_BIN) -sverilog -Mdir=$(VCS_OBJDIR)
VCS_OBJDIR = $(VCS_DIR)/obj
VCS_BIN = $(VCS_DIR)/simv

# iverilog specific
IVERILOG_DIR = $(BUILD_DIR)/iverilog
IVERILOG_FLAGS =
IVERILOG_BIN =

#--------------------disgn specific------------------
DESIGN := GCD#can be overrided in command line

# include scripts
include $(SCRIPTS_DIR)/elaborate.mk

include $(SCRIPTS_DIR)/sim-sv-verilator.mk

include $(SCRIPTS_DIR)/sim-sv-vcs.mk

include $(SCRIPTS_DIR)/sim-cpp-verilator.mk

include $(SCRIPTS_DIR)/sim-uvm-vcs.mk

include $(SCRIPTS_DIR)/sim-uvm-verilator.mk

include $(SCRIPTS_DIR)/sim-python.mk

vcd: 
	$(WAVE_VIEWER) $(VCD_FILE) &

fst:
	$(WAVE_VIEWER) $(FST_FILE) &

	

sim-chisel-verilator:
sim-chisel-vcs:
-include ../Makefile
