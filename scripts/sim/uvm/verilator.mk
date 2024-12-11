# now it has serveral disadvantage
# 1, have to recompile each time you run(i dont know how to get cache)
# 2, verialtor can't fully support sv/uvm
.PHONY: sim
sim: $(UVM_TB_SRCS) $(VSRCS) verilog
	UVM_HOME=$(DEP_DIR)/uvm-verilator/src \
	TB_DIR=$(TB_DIR) \
	$(SIM_DIR)/uvm/run_verilator.sh test