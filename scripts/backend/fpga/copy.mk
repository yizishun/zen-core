AX7020_DIR = $(SYN_DIR)/fpga/AX7020
NVBOARD_DIR = $(SYN_DIR)/fpga/nvboard
HDUBOARD_DIR = $(SYN_DIR)/fpga/xc7a100tfgg484-2L
#TODO: auto generate vivado project
.PHONY:AX7020
AX7020:
	mkdir -p $(AX7020_DIR)/vsrc
	mkdir -p $(AX7020_DIR)/constr
	cp $(RTL_FILES) $(AX7020_DIR)/vsrc

#TODO:nvboard

