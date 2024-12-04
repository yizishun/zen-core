# cpp -> a -------------------------
VERLATOR_INC = $(VERILATOR_HOME)/include
CSRCS = $(shell find $(CHISEL_TB_DIR)/clib -maxdepth 1 -name "*.cpp")
OBJS = $(CSRCS:.cpp=.o)  # 将所有 .cpp 文件替换为对应的 .o 文件
LIBC_A = $(CHISEL_TB_DIR)/clib/libchisel.a
CXXFLAGS = -std=c++11 -fPIC -O2 -I$(CHISEL_TB_DIR)/clib/json/include -I$(CHISEL_TB_DIR)/clib/include -I$(VERLATOR_INC) -DCONFIG_FILE='"$(CONFIG_DIR)/$(DESIGN)TestBench.json"'

libchisel: $(LIBC_A)
$(LIBC_A): $(OBJS)
	@echo "+ $@"
	ar rcs $@ $^
	rm -f $(OBJS)

%.o: %.cpp
	@echo "+ $< -> $@"
	g++ $(CXXFLAGS) -c $< -o $@

cleanlibchisel:
	rm -f $(LIBC_A)

# sim target -----------------------
sim-chisel-verilator: $(LIBC_A) $(CSRCS) tb-verilog
	mkdir -p $(VERILATOR_DIR)
	verilator \
		$(VERILATOR_FLAGS) --main --vpi \
		$(CHISEL_TB_INC) \
		$(CHISEL_TB_SRCS)
	$(VERILATOR_BIN)

sim-chisel-vcs:
	mkdir -p $(VCS_DIR)
	mkdir -p $(VCS_OBJDIR)
	vcs \
		$(VCS_FLAGS) \
		$(CHISEL_TB_INC) \
		$(CHISEL_TB_SRCS)
	$(VCS_BIN)
	$(VERDI_HOME)/bin/fsdb2vcd $(BUILD_DIR)/wave.fsdb -o $(VCD_FILE)
	rm -rf $(VCS_DIR)/fsdb2vcdLog
	mv ./fsdb2vcdLog $(VCS_DIR)/fsdb2vcdLog
