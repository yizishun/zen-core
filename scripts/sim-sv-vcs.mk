VCS_INC = +incdir+$(abspath $(TB_DIR)/sv-tb)
sim-sv-vcs:
	$(call git_commit, "sim RTL")
	mkdir -p $(VCS_DIR)
	vcs $(VCS_FLAGS) \
		$(VCS_INC) \
		$(SV_TB_SRCS) $(VSRCS)
	$(VCS_BIN)
	$(VERDI_HOME)/bin/fsdb2vcd $(BUILD_DIR)/wave.fsdb -o $(VCD_FILE)
	rm -rf $(VCS_DIR)/fsdb2vcdLog
	mv ./fsdb2vcdLog $(VCS_DIR)/fsdb2vcdLog
