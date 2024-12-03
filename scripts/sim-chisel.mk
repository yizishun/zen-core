CSRCS = $(CHISEL_TB_DIR)/clib/dpi.cpp
sim-chisel-verilator: tb-verilog
	mkdir -p $(VERILATOR_DIR)
	verilator \
		$(VERILATOR_FLAGS) --main \
		$(CHISEL_TB_INC) \
		$(CHISEL_TB_SRCS)
	$(VERILATOR_BIN)

sim-chisel-vcs:
	mkdir -p $(VCS_DIR)
	mkdir -p $(VCS_OBJDIR)
	vcs \
		$(VCS_FLAGS) \
		$(CHISEL_TB_INC) \
		$(CHISEL_TB_SRC)
	$(VCS_BIN)
