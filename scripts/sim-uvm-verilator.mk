#not work, dont use
sim-uvm-verilator: $(UVM_TB_SRCS) $(VSRCS) verilog
	UVM_HOME=./uvm-verilator/src \
	TB_DIR=$(UVM_TB_DIR) \
	$(SCRIPTS_DIR)/run_verilator.sh test
