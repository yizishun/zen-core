
sim-cpp-verilator: $(VSRCS) $(CPP_TB_SRCS) verilog
	$(eval TOP := GCD)
	verilator $(VERILATOR_FLAGS) \
		$(VSRCS) $(CPP_TB_SRCS)
	$(VERILATOR_BIN)