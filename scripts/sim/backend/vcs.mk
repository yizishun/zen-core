.PHONY: sim
sim:
	mkdir -p $(VCS_DIR)
	mkdir -p $(VCS_OBJDIR)
	vcs \
		$(VCS_FLAGS) \
		$(TB_INC) \
		$(TB_SRCS)
	$(VCS_BIN)
	$(VERDI_HOME)/bin/fsdb2vcd $(BUILD_DIR)/wave.fsdb -o $(VCD_FILE)
	rm -rf $(VCS_DIR)/fsdb2vcdLog
	mv ./fsdb2vcdLog $(VCS_DIR)/fsdb2vcdLog