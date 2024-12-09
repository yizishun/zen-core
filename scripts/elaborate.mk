#rtl
RTL_DIR = $(BUILD_DIR)/rtl
RTL_LIST = $(RTL_DIR)/filelist.f
ELABORATE_DIR = $(RTL_DIR)/elaborate
#tb
TB_RTL_DIR = $(CHISEL_TB_DIR)/build
TB_RTL_LIST = $(TB_RTL_DIR)/filelist.f
TB_ELABORATE_DIR = $(TB_RTL_DIR)/elaborate
#fir files
FIR_FILES = $(shell find $(abspath $(ELABORATE_DIR)) -name "*.fir")
TB_FIR_FILES = $(shell find $(abspath $(TB_ELABORATE_DIR)) -name "*.fir")
#firtool options
FIRTOOL_OPTION = \
	-O=debug \
	--split-verilog \
	--preserve-values=all \
  --lowering-options=verifLabels,omitVersionComment,disallowLocalVariables,disallowPackedArrays,locationInfoStyle=wrapInAtSquareBracket \
  --strip-debug-info

.PHONY: config
config:
	mkdir -p $(CONFIG_DIR)
	mill -i elaborateRTL.runMain elaborate.Elaborate_$(DESIGN) config --width 32 --useAsyncReset true --target-dir config

.PHONY: fir
fir: config
	mkdir -p $(ELABORATE_DIR)
	mill -i elaborateRTL.runMain elaborate.Elaborate_$(DESIGN) design --target-dir $(ELABORATE_DIR) --parameter ./config/$(DESIGN).json

.PHONY: verilog
verilog: fir
	mkdir -p $(RTL_DIR)
	for file in $(FIR_FILES); do \
		basename=$$(basename $$file .fir); \
		anno_file=$(ELABORATE_DIR)/$$basename.anno.json; \
		firtool $(FIRTOOL_OPTION) --annotation-file $$anno_file $$file -o $(RTL_DIR); \
	done
	find $(RTL_DIR) -maxdepth 1 -name "*.sv" -type f -print > $(RTL_LIST)

.PHONY: tb-fir
tb-fir:
	mkdir -p $(TB_ELABORATE_DIR)
	mill -i elaborateTB.runMain $(DESIGN)TestBenchMain design --target-dir $(TB_ELABORATE_DIR) --parameter ./config/$(DESIGN)TestBench.json

.PHONY: tb-verilog
tb-verilog: tb-fir
	mkdir -p $(TB_RTL_DIR)
	for file in $(TB_FIR_FILES); do \
    basename=$$(basename $$file .fir); \
    anno_file=$(TB_ELABORATE_DIR)/$$basename.anno.json; \
    firtool $(FIRTOOL_OPTION) --annotation-file $$anno_file $$file -o $(TB_RTL_DIR); \
	done
	find $(TB_RTL_DIR) -maxdepth 1 -name "*.sv" -type f -print > $(TB_RTL_LIST)


help:
	mill -i elaborateRTL.run --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

bsp:
	mill -i mill.bsp.BSP/install

idea:
	mill -i mill.idea.GenIdea/idea

clean:
	-rm -rf $(BUILD_DIR)

clean-tb:
	-rm -rf $(TB_RTL_DIR)

clean-rtl:
	-rm -rf $(RTL_DIR)

.PHONY: test verilog help reformat checkformat clean