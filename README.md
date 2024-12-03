Digital Design/verify Template
=======================
This is a template or framework include digital design and verification

# Design
This template is originnally built from chisel-playground
So, Digital design is use chisel HDL language
## Write Chisel
* Pls use serializableModule and serializableParameter to construcr module. refer to gcd example to know(actually you can just relace `GCD` to your module name, and change the impl)
* Write in `./rtl/src`
* Write module parameter in `./config`, in json format. And the file name is corresponding module name which will be elaborated
## Elaborate Chisel
* Write corresponding(top level) module elaborate code in `./elaborateRTL`
* Naming the elaborate class `Elaborate_{module name}`(pls read the `./script/elaborate.mk` to know why)
* Use serializableElaborate extend your class 
* Other modify pls refer `./elaborate/Elaborate_gcd.scala` (actually you can just relace `GCD` to your module name, and change the impl)
* Finally, use `make verilog` to get the verilog, use `make fir` to get the firrtl

You can override the variable DESIGN in command line to elaborate and verify different Design

# Verify
The template support lots of testbench framework(language) and lots of simulator

For corresponding target, use `make sim-{tb}-{simulator}` to run

Write TestBench in `tb/tb-{DEIGN}`

(Before use vcs, make sure you already define `VCS_HOME` and `VERDI_HOME`)
## UVM
uvm support:
`make sim-uvm-vcs`
`make sim-uvm-verilator`(have bugs, because verilator is not fully support SystemVerilog, i support it use `uvm-verialtor`)

## SV
Similar to UVM, support:
`make sim-sv-vcs`
`make sim-uvm-verilator`(have some dump wave bugs)

## C++
Only for verilator:
`make sim-cpp-verilator`

## COCOTB
support lots of simulator:
`make sim-python-iverilog`
`make sim-python-verilator`
`make sim-python-vcs` (not test, dont use)
cocotb is useful for small design verification (for example, sub module in a Core)

## Chisel
under deveplopment(this should use the main branch chisel to support) (it is not chisel test, it is acually use chisel to write tb and elaborate to verilog/sv)

# Conclusion
available testbench:
```shell
# Init
git submodule update --init --recursive
# Elaborate RTL
make verilog DEIGN=GCD
# UVM
sim-uvm-vcs
sim-uvm-verilator(bugs)
# SV
sim-sv-vcs
sim-sv-verilator(bugs)
# CPP
sim-cpp-verilator
# COCOTB
sim-python-iverilog
sim-python-verilator
# Chisel
```
