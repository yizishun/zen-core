#UVM_TB_SRCS = $(shell find $(abspath $(UVM_TB_DIR)) -name "*.v" -or -name "*.sv" -or -name "*.cpp" -or -name "*.cc" -or -name "*.c")
TB_SRCS += $(shell find $(abspath $(TB_DIR)) -maxdepth 1 -name "*.v" -or -name "*.sv") #tb and pkgs files
TB_SRCS += $(shell find $(abspath $(TB_DIR)) -name "interface.sv") # interface can not in pkgs
TB_SRCS += $(shell find $(abspath $(TB_DIR)) -name "*.a") # dpi files
VSRCS = $(shell find $(abspath $(RTL_DIR)) -maxdepth 1 -name "*.v" -or -name "*.sv")
VSRCS += $(foreach layers, $(LAYERS.$(SIM)), $(call collect_layer, $(RTL_DIR)/$(layers)))
TB_SRCS += $(VSRCS)
TB_INC = +incdir+$(abspath $(TB_DIR)) +incdir+$(abspath $(RTL_DIR))

VCS_FLAGS += -ntb_opts uvm-1.2
