# now it has serveral disadvantage
# 1, have to recompile each time you run(i dont know how to get cache)
# 2, verialtor can't fully support sv/uvm
sim-uvm-verilator: $(UVM_TB_SRCS) $(VSRCS) verilog
	UVM_HOME=./uvm-verilator/src \
	TB_DIR=$(UVM_TB_DIR) \
	$(SCRIPTS_DIR)/run_verilator.sh test
