TB_SRCS = $(shell find $(abspath $(TB_DIR)) -name "*.v" -or -name "*.sv" -or -name "*.a") #tb
TB_SRCS += $(shell find $(abspath $(RTL_DIR)) -maxdepth 1 -name "*.v" -or -name "*.sv") #rtl
TB_SRCS += $(foreach layers, $(LAYERS.$(SIM)), $(call collect_layer, $(RTL_DIR)/$(layers))) #rtl layers
TB_INC = +incdir+$(abspath $(TB_DIR)) +incdir+$(abspath $(RTL_DIR))

VERILATOR_FLAGS += --main
