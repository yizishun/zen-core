.PHONY: sim-sv-vcs sim-sv-verilator
sim-sv-vcs:
	$(call git_commit, "sim RTL")
	mkdir -p $(VCS_DIR)
	vcs $(VCS_FLAGS) \
		$(SV_TB_INC) \
		$(SV_TB_SRCS) $(VSRCS)
	$(VCS_BIN)
	$(VERDI_HOME)/bin/fsdb2vcd $(BUILD_DIR)/wave.fsdb -o $(VCD_FILE)
	rm -rf $(VCS_DIR)/fsdb2vcdLog
	mv ./fsdb2vcdLog $(VCS_DIR)/fsdb2vcdLog

#this have dump wave bugs
sim-sv-verilator: $(SV_TB_SRCS) $(VSRCS) verilog
	$(call git_commit, "sim RTL")
	$(eval VERILATOR_INC := -I$(abspath $(TB_DIR)/sv-tb))
	verilator $(VERILATOR_FLAGS) --main \
		$(VERILATOR_INC) \
		$(SV_TB_SRCS) $(VSRCS)
	$(VERILATOR_BIN)
	mv $(BUILD_DIR)/wave $(BUILD_DIR)/wave.vcd
