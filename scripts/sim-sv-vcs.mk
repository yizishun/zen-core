VCS_INC = +incdir+$(abspath $(TB_DIR)/sv-tb)
sim-sv-vcs:
	$(call git_commit, "sim RTL")
	mkdir -p $(VCS_DIR)
	vcs -full64 -timescale=1ns/1ns -debug_access+all -l \
		$(VCS_DIR)/vcs.log -o $(VCS_BIN) \
	 	-sverilog \
		$(VCS_INC) \
		-Mdir=$(VCS_OBJDIR) \
		$(SV_TB_SRCS) $(VSRCS)
	$(VCS_BIN)
	$(VERDI_HOME)/bin/fsdb2vcd $(BUILD_DIR)/wave.fsdb -o $(VCD_FILE)
	rm -rf $(VCS_DIR)/fsdb2vcdLog
	mv ./fsdb2vcdLog $(VCS_DIR)/fsdb2vcdLog
