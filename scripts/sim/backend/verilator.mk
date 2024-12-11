#TODO: auto run `make verilog` or `make tb-verilog`
.PHONY: sim
sim: $(TB_SRCS)
	mkdir -p $(VERILATOR_DIR)
	verilator \
		$(VERILATOR_FLAGS) \
		$(TB_INC) \
		$(TB_SRCS)
	$(VERILATOR_BIN)
	-mv $(BUILD_DIR)/wave $(BUILD_DIR)/wave.vcd
