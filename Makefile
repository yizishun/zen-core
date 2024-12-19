TOP := TB
ROOT := $(shell pwd)
#common DIR
BUILD_DIR = $(abspath ./build)
SCRIPTS_DIR = $(abspath ./scripts)
TB_DIR = $(abspath ./tb/tb-$(DESIGN)/$(TBLANG)-tb)
DEP_DIR = $(abspath ./dependencies)
CONFIG_DIR = $(abspath ./config)
CONFIG_FILE = $(shell find $(CONFIG_DIR) -name "$(DESIGN).json")
SYN_DIR = $(abspath ./backend)

#common files
VCD_FILE = $(BUILD_DIR)/wave.vcd
FST_FILE = $(BUILD_DIR)/wave.fst
WAVE_VIEWER = gtkwave

#-----------override it in command line-----------
DESIGN := ALU
DPATH := zen.backend.fu.
TBLANG := chisel
SIM := verilator

$(info DESIGN: $(DESIGN))
$(info TB_DIR: $(TB_DIR))

export DESIGN

# include scripts
include $(SCRIPTS_DIR)/design/elaborate.mk

include $(SCRIPTS_DIR)/sim/sim.mk

include $(SCRIPTS_DIR)/backend/yosys-sta.mk

include $(SCRIPTS_DIR)/formal/formal.mk

#common targets
.PHONY: vcd
vcd: 
	$(WAVE_VIEWER) $(VCD_FILE) &

.PHONY: fst
fst:
	$(WAVE_VIEWER) $(FST_FILE) &

.PHONY: init
init:
	@git submodule update --init --recursive
	@echo "submdule init -- done"
	@poetry install --no-root
	@echo "poetry install -- done"
-include ../Makefile
