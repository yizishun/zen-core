VSRCS += $(shell find $(abspath $(TB_DIR)/build) -maxdepth 1 -name "*.v" -or -name "*.sv")
VSRCS += $(foreach layers, $(LAYERS.$(SIM)), $(call collect_layer, $(TB_DIR)/build/$(layers))) #layers
TB_SRCS += $(VSRCS)
TB_SRCS += $(shell find $(abspath $(TB_DIR)) -name "*.a") #dpi
TB_INC = +incdir+$(abspath $(TB_DIR)/build) -CFLAGS -I$(abspath $(TB_DIR)/clib/include)

VERILATOR_FLAGS += --main --vpi

##support verilator vcs
