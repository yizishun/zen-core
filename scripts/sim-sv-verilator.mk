#this have bugs
sim-sv-verilator: $(SV_TB_SRCS) $(VSRCS) verilog
	$(call git_commit, "sim RTL")
	$(eval VERILATOR_INC := -I$(abspath $(TB_DIR)/sv-tb))
	verilator $(VERILATOR_FLAGS) --main \
		$(VERILATOR_INC) \
		$(SV_TB_SRCS) $(VSRCS)
	$(VERILATOR_BIN)
	mv $(BUILD_DIR)/wave $(BUILD_DIR)/wave.vcd