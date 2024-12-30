set_app_var fml_mode_on true
set_app_var enable_verdi_debug true
set_app_var verdi_export_dir ./trace
set_fml_var fml_quiet_trace false

read_file -top IFUFormal -format sverilog -sva -vcs {-full64 -sverilog -f /mnt/zen-core/tb/tb-ifu/chisel-tb/build/filelist.f +incdir+/mnt/zen-core/tb/tb-ifu/chisel-tb/build}

create_clock clock -period 833

create_reset reset -high

set_fml_var fml_witness_on true

set_constant PWROK -value 1

sim_run -stable

sim_save_reset

check_fv

report_fv -list

sim_time

save_session
