Digital Design/verify Template
=======================
This is a template or framework include digital design and verification
# Quick Start
```shell
# Init
make init
# Elaborate RTL
make verilog DEIGN=GCD
# Create dpi-lib
make dpi-lib DEIGN=GCD TBLANG=xxx
# UVM
make sim DESIGN=GCD TBLANG=uvm SIM=vcs
make sim DESIGN=GCD TBLANG=uvm SIM=verilator #(bugs)
# SV
make sim DESIGN=GCD TBLANG=sv SIM=vcs
make sim DESIGN=GCD TBLANG=sv SIM=verilator #(bugs)
# CPP
make sim DESIGN=GCD TBLANG=cpp SIM=verilator
# COCOTB
make sim DESIGN=GCD TBLANG=cocotb SIM=icarus
make sim DESIGN=GCD TBLANG=cocotb SIM=verilator
# Chisel
make tb-verilog DEIGN=GCD
make sim DESIGN=GCD TBLANG=chisel SIM=verilator #(bugs)
make sim DESIGN=GCD TBLANG=chisel SIM=vcs
```

# Design
This template is originnally built from chisel-playground
So, Digital design is use chisel HDL language
## Write Chisel
* Pls use serializableModule and serializableParameter to construcr module. refer to gcd example to know(actually you can just relace `GCD` to your module name, and change the impl)
* Write in `./rtl/src`
* Write module parameter in `./config`, in json format. And the file name is corresponding module name which will be elaborated
## Elaborate Chisel
* Write corresponding(top level) module elaborate code in `./elaborateRTL`
* Naming the elaborate class `Elaborate_{module name}`(pls read the `./script/design/elaborate.mk` to know why)
* Use serializableElaborate extend your class 
* Other modify pls refer `./elaborate/Elaborate_gcd.scala` (actually you can just relace `GCD` to your module name, and change the impl)
* Finally, use `make verilog` to get the verilog, use `make fir` to get the firrtl

You can override the variable DESIGN in command line to elaborate and verify different Design

# Verify
The template support lots of testbench framework(language) and lots of simulator

For corresponding target, set `TBLANG` and `SIM`

Write TestBench in `tb/tb-{DEIGN}`
Write dpi-c in `tb/tb-{DEIGN}/{TBLANG}-tb/dpi`

(Before use vcs, make sure you already define `VCS_HOME` and `VERDI_HOME`)


# TODO
* [ ]Difftest
  * [ ] 在写完core之后尝试添加difftest
  * [ ] 尝试把difftest跑起来
  * [ ] 如果跑不起来，把所有NOOP_HOME的依赖去掉
  (这个difftest的原理呢，其实就是在提供了一些内嵌于chisel的模块方法呀之类的，然后在需要difftest的时候使用DifftestMdule例化一个特定功能的模块（详见readme的api部分），然后你讲这个模块的输出和输入连接到你的dut上，最后emitverilog的结果也其实就是verilog的模块，然后他还会自动生成一些dpi-c函数，如果想要跑仿真呢，我感觉就应该需要把这个函数和所有module传给verilator就行，然后他difftest有一个现成的emu（verilator），emu-run，simv（vcs） tartget，使用这些target可以跑他现成的仿真templete，但是我现在疑惑最主要的就是这个NOOP_HOME在difftest出现了很多次，这个为啥没做到节藕呢？不理解，然后还有一个问题，就是我觉得生成的模块应该是pure的，就是core相关的代码不能有一点验证相关的部分（比如difftest实际就是验证代码），我希望验证代码能和设计代码节藕，我想到了三种方法，第一种，在TB.scala中例化相关的difftest module，然后使用probe访问各种信号（这需要在定义设计模块时提前添加好probe）,第二种，在设计代码中设计layer（自定义layer），把difftest相关的代码放在某个layer中，然后仿真时选择性包含就行，第三种，用config来选择性例化所有difftest模块，但是这个感觉有点麻烦，因为需要很多config选项，实际上这首先涉及difftest的开关方式，第1，3种都是在json文件中开关的，而第2种则是通过makefile（layer是这样的），第一种很好，唯一不好的点就是只有scala tb可以用，假如你想用uvm来测试你的代码，你就没法开difftest，而第二三种都是讲difftest内嵌入设计，所有tb就可以灵活选择，所以现在基本上来看，就是第二种maybe会好一点)

迁移计划：
* [ ] 0. 尝试迁移xiangshan工具链
* [ ] 1. 把所有模块迁移（可以边迁移边编写tb然后边分析面积）
* [ ] 1.5 尝试迁移xiangshan工具链
* [ ] 2. 迁移cpp-tb
* [ ] 3. 迁移nvboard相关配置
* [ ] 4. 迁移pf-trace相关配置
* [ ] 5. 迁移makefile等配置
* [ ] 6. 修改readme