
sim-uvm-vcs:
	$(call git_commit, "sim RTL")
	mkdir -p $(VCS_DIR)
	vcs $(VCS_FLAGS) \
		$(UVM_TB_INC) \
		-ntb_opts uvm-1.2 \
		$(UVM_TB_SRCS) $(VSRCS)
	$(VCS_BIN)
	$(VERDI_HOME)/bin/fsdb2vcd $(BUILD_DIR)/wave.fsdb -o $(VCD_FILE)
	rm -rf $(VCS_DIR)/fsdb2vcdLog
	mv ./fsdb2vcdLog $(VCS_DIR)/fsdb2vcdLog

# now it has serveral disadvantage
# 1, have to recompile each time you run(i dont know how to get cache)
# 2, verialtor can't fully support sv/uvm
sim-uvm-verilator: $(UVM_TB_SRCS) $(VSRCS) verilog
	UVM_HOME=./uvm-verilator/src \
	TB_DIR=$(UVM_TB_DIR) \
	$(SCRIPTS_DIR)/run_verilator.sh test
