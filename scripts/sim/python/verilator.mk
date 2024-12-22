sim:
	$(eval SIM=verilator)
	$(eval SIM_BUILD = $(VERILATOR_DIR))
	$(eval EXTRA_ARGS += -CFLAGS -std=c++20)
	$(eval EXTRA_ARGS += --trace --trace-structs)
	poetry run \
		make -C $(TB_DIR) \
			-f $(COCOTB_MAKEFILE)
	mv $(TB_DIR)/dump.vcd $(BUILD_DIR)/wave.vcd