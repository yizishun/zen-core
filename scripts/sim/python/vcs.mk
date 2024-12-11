#dont use,i dont have test environment
sim:
	$(eval SIM=vcs)
	poetry run \
		make -C $(TB_DIR) \
			-f $(COCOTB_MAKEFILE) \
