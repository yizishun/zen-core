
sim-cpp-verilator: $(VSRCS) $(CPP_TB_SRCS) verilog
	$(eval TOP := $(DESIGN_UP))
	verilator $(VERILATOR_FLAGS) \
		$(VSRCS) $(CPP_TB_SRCS)
	$(VERILATOR_BIN)