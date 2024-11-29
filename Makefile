TOP := TB
BUILD_DIR = ./build
SCRIPTS_DIR = ./scripts
TB_DIR = ./tb-$(DESIGN)

#common files
VSRCS = $(shell find $(abspath $(BUILD_DIR)) -name "*.v" -or -name "*.sv")
VCD_FILE = $(BUILD_DIR)/wave.vcd

#------------------language specific --------------
#sv tb specific
SV_TB_DIR = $(TB_DIR)/sv-tb
SV_TB_SRCS = $(shell find $(abspath $(SV_TB_DIR)) -name "*.v" -or -name "*.sv" -or -name "*.cpp" -or -name "*.cc" -or -name "*.c")

#cpp tb specific
CPP_TB_DIR = $(TB_DIR)/cpp-tb
CPP_TB_SRCS = $(shell find $(abspath $(CPP_TB_DIR)) -name "*.cpp" -or -name "*.cc" -or -name "*.c")

#-------------------simulator specific--------------
# verilator specific
VERILATOR_DIR = $(BUILD_DIR)/verilator
VERILATOR_FLAGS = --trace --timing --threads 8 -O1 --build --exe -cc --top $(TOP) -Mdir $(VERILATOR_DIR)
VERILATOR_BIN = $(VERILATOR_DIR)/V$(TOP)

# vcs specific
VCS_DIR = $(BUILD_DIR)/vcs
VCS_OBJDIR = $(VCS_DIR)/obj
VCS_BIN = $(VCS_DIR)/simv
VCS_FLAGS = vcs -full64 -timescale=1ns/1ns -debug_access+all -l $(VCS_DIR)/vcs.log -o $(VCS_BIN) -sverilog -Mdir=$(VCS_OBJDIR)

#--------------------disgn specific------------------
DESIGN := gcd
DESIGN_UP = $(shell echo $(DESIGN) | tr '[:lower:]' '[:upper:]')

# include scripts
include $(SCRIPTS_DIR)/elaborate.mk

include $(SCRIPTS_DIR)/sim-sv-verilator.mk

include $(SCRIPTS_DIR)/sim-sv-vcs.mk

include $(SCRIPTS_DIR)/sim-cpp-verilator.mk

vcd: 
	gtkwave $(VCD_FILE) &

sim-uvm-vcs:

sim-chisel-verilator:
sim-chisel-vcs:
sim-python-verilator:
sim-python-vcs:
-include ../Makefile
