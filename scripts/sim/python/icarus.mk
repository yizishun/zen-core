
.PHONY: sim
sim: verilog
	$(eval SIM=icarus)
	$(eval SIM_BUILD = $(IVERILOG_DIR))
	poetry run \
		make -C $(TB_DIR) \
			-f $(COCOTB_MAKEFILE)
	mv $(IVERILOG_DIR)/$(DESIGN).fst $(BUILD_DIR)/wave.fst