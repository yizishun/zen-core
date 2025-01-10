package zen.frontend

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import org.chipsalliance.rvdecoderdb
//有三个东西
//1.rvdecoderdb.Instruction 这包含了一条指令的相关结构化信息
//2.InstructionPattern 这是一个pattern
//3.PossibleInput 这个返回所需要指令的pattern seq
//(4.attribute 通过解析Instruction对象生成的attribute)

//pattern templete
case class InstructionPattern(
  val inst: rvdecoderdb.Instruction
) extends DecodePattern {
  def bitPat: BitPat = BitPat(s"b${inst.encoding.toString()}")
}

//assist method
object InstructionPattern {
  implicit class addMethodsToInsn(i: InstructionPattern) {
    def hasArg(arg: String) = i.inst.args.map(_.name).contains(arg)
  }
}

//pattern seq
object PossibleInput {
  def apply(rvopcodePath: os.Path): Seq[InstructionPattern] = {
    val instList = rvdecoderdb.instructions(rvopcodePath)
    //一些过滤条件
    val instSets = Set("rv_i", "rv_zicsr", "rv_system", "rv_zifencei")
    val ex_rv_i = Set("fence")
    val ex_rv_zicsr = Set("csrrc", "csrrwi", "csrrsi", "csrrci")
    val ex_rv_system = Set("wfi")
    val patternSeq = instList
      .filter(_.pseudoFrom.isEmpty) //伪指令不考虑
      .filter(inst => instSets.contains(inst.instructionSet.name))
      .filter(inst => !ex_rv_i.contains(inst.name))
      .filter(inst => !ex_rv_zicsr.contains(inst.name))
      .filter(inst => !ex_rv_system.contains(inst.name))
      .map(InstructionPattern(_))
      .toSeq
    patternSeq
  }
}
