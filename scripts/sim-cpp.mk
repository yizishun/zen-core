.PHONY: sim-cpp-verilator
sim-cpp-verilator: $(VSRCS) $(CPP_TB_SRCS) verilog
	$(eval TOP := $(DESIGN))
	verilator $(VERILATOR_FLAGS) \
		$(VSRCS) $(CPP_TB_SRCS)
	$(VERILATOR_BIN)