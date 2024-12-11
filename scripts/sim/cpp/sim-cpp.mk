VSRCS = $(shell find $(abspath $(RTL_DIR)) -maxdepth 1 -name "*.v" -or -name "*.sv")
VSRCS += $(foreach layers, $(LAYERS.$(SIM)), $(call collect_layer, $(RTL_DIR)/$(layers)))

TB_SRCS = $(shell find $(abspath $(TB_DIR)) -name "*.cpp" -or -name "*.cc" -or -name "*.c")
TB_SRCS += $(VSRCS)
TB_INC = -I$(TB_DIR)/include

override TOP = $(DESIGN)

#support verilator