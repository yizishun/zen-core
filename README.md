# ZEN-V Core(禅芯 Riscv)


# TODO
* [ ] 1. 把所有模块迁移（可以边迁移边编写tb然后边分析面积）
* [ ]Difftest
  * [ ] 在写完core之后尝试添加difftest
  * [ ] 尝试把difftest跑起来
  * [ ] 如果跑不起来，把所有NOOP_HOME的依赖去掉
  (这个difftest的原理呢，其实就是在提供了一些内嵌于chisel的模块方法呀之类的，然后在需要difftest的时候使用DifftestMdule例化一个特定功能的模块（详见readme的api部分），然后你讲这个模块的输出和输入连接到你的dut上，最后emitverilog的结果也其实就是verilog的模块，然后他还会自动生成一些dpi-c函数，如果想要跑仿真呢，我感觉就应该需要把这个函数和所有module传给verilator就行，然后他difftest有一个现成的emu（verilator），emu-run，simv（vcs） tartget，使用这些target可以跑他现成的仿真templete，但是我现在疑惑最主要的就是这个NOOP_HOME在difftest出现了很多次，这个为啥没做到节藕呢？不理解，然后还有一个问题，就是我觉得生成的模块应该是pure的，就是core相关的代码不能有一点验证相关的部分（比如difftest实际就是验证代码），我希望验证代码能和设计代码节藕，我想到了三种方法，第一种，在TB.scala中例化相关的difftest module，然后使用probe访问各种信号（这需要在定义设计模块时提前添加好probe）,第二种，在设计代码中设计layer（自定义layer），把difftest相关的代码放在某个layer中，然后仿真时选择性包含就行，第三种，用config来选择性例化所有difftest模块，但是这个感觉有点麻烦，因为需要很多config选项，实际上这首先涉及difftest的开关方式，第1，3种都是在json文件中开关的，而第2种则是通过makefile（layer是这样的），第一种很好，唯一不好的点就是只有scala tb可以用，假如你想用uvm来测试你的代码，你就没法开difftest，而第二三种都是讲difftest内嵌入设计，所有tb就可以灵活选择，所以现在基本上来看，就是第二种maybe会好一点)
  (但是我发现他这个difftest的默认emu目标（也就是构建verilator）的脚本甚至被香山采用了，有点意思，在移植完所有模块后好好看看，看能不能直接用他的，而且lightSSS就是在这个框架里面的)
  (甚至不止lightSSS，还有chiselDB，这个constantin似乎也还不错)

迁移计划：
* [ ] 2. 迁移cpp-tb
* [ ] 3. 迁移nvboard相关配置
* [ ] 4. 迁移pf-trace相关配置
* [ ] 5. 迁移makefile等配置
* [ ] 6. 修改readme