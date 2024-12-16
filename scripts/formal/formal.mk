PHONY: sby
sby:
	-sby -f tb/tb-$(DESIGN)/$(DESIGN).sby
	-mv tb/tb-$(DESIGN)/$(DESIGN)_basic/engine_0/trace.vcd $(BUILD_DIR)/wave.vcd

export VC_STATIC_HOME=$(VCS_HOME)/vcfca
PHONY: vcf
vcf:
	find $(TB_RTL_DIR) -name "*.sv" -type f -print > $(TB_RTL_LIST)
	$(VCS_HOME)/vcfca/bin/vcf -f tb/tb-$(DESIGN)/$(DESIGN).tcl -verdi &

restore:
	$(VCS_HOME)/vcfca/bin/vcf -restore -verdi
