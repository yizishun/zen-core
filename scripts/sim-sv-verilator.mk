VERILATOR_DIR = $(BUILD_DIR)/verilator
VERILATOR_FLAGS = --trace --timing --threads 8 -O1 --main --build --exe -cc --top $(TOP)
VERILATOR_INC = -I$(abspath $(TB_DIR)/sv-tb)
VERILATOR_BIN = $(VERILATOR_DIR)/V$(TOP)

#this have bugs
sim-sv-verilator: $(TB_SRCS) $(VSRCS) verilog
	$(call git_commit, "sim RTL")
	verilator $(VERILATOR_FLAGS) \
		$(VERILATOR_INC) \
		-Mdir $(VERILATOR_DIR) \
		$(TB_SRCS) $(VSRCS)
	$(VERILATOR_BIN)
	mv $(BUILD_DIR)/wave $(BUILD_DIR)/wave.vcd