package zen.frontend

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import org.chipsalliance.rvdecoderdb
import zen.backend.fu._
import zen._

trait HasCatgory{
  val aluAddInst = Set("add", "addi", "lui", "auipc")
  val aluSubInst = Set("sub")
  val aluAndInst = Set("and", "andi")
  val aluOrInst  = Set("or", "ori")
  val aluXorInst = Set("xor", "xori")
  val aluSllInst = Set("sll")
  val aluSrlInst = Set("srl")
  val aluSraInst = Set("sra")
  val aluCmpInst = Set("slt", "slti")
  val aluCmpuInst= Set("sltu", "sltiu")
  val aluInst = Set(
    aluAddInst ++ aluSubInst ++ aluAndInst ++ aluOrInst ++ aluXorInst ++ aluSllInst ++ aluSrlInst ++ aluSraInst ++ aluCmpInst ++ aluCmpuInst
  ).flatten
  val csrWrtInst  = Set("csrrw")
  val csrSetInst  = Set("csrrs")
  val csrMretInst = Set("mret")
  val csrEcallInst= Set("ecall")
  val csrInst = Set(
    csrWrtInst ++ csrSetInst ++ csrMretInst ++ csrEcallInst
  ).flatten
  val lsuLoadInst = Set("lb", "lh", "lw", "lbu", "lhu")
  val lsuStoreInst= Set("sb", "sh", "sw")
  val lsuInst = Set(
    lsuLoadInst ++ lsuStoreInst
  ).flatten
  val brJInst =   Set("jal", "jalr")
  val brBEInst =  Set("beq", "bne")
  val brBGLInst = Set("blt", "bge", "bltu", "bgeu")
  val branchInst =Set(
    brJInst ++ brBEInst ++ brBGLInst
  ).flatten
}

object Fu extends DecodeField[InstructionPattern, UInt] with HasCatgory{
  def name = "fu"
  def chiselType = UInt(2.W)
  def genTable(instPattern: InstructionPattern): BitPat = {
    import zen.backend.FU
    instPattern.inst.name match {
      case name if aluInst.contains(name) || branchInst.contains(name) => BitPat(FU.ALU)
      case name if csrInst.contains(name) => BitPat(FU.CSR) 
      case name if lsuInst.contains(name) => BitPat(FU.LSU)
      case _ => BitPat(FU.ALU)
    }
  }
}

object Func extends DecodeField[InstructionPattern, UInt] with HasCatgory{
  def name = "func"
  def chiselType = UInt(4.W)
  def genTable(instPattern: InstructionPattern): BitPat = {
    instPattern.inst.name match {
      case name if aluInst.contains(name) => genAluFunc(instPattern)
      case name if csrInst.contains(name) => genCsrFunc(instPattern)
      case name if lsuInst.contains(name) => genLsuFunc(instPattern)
      case _ => BitPat(0.U(4.W))
    }
  }
  def genAluFunc(instPattern: InstructionPattern): BitPat = {
    instPattern.inst.name match {
      case name if aluAddInst.contains(name) => BitPat(AluOp.ALU_ADD)
      case name if aluSubInst.contains(name) => BitPat(AluOp.ALU_SUB)
      case name if aluAndInst.contains(name) => BitPat(AluOp.ALU_AND)
      case name if aluOrInst.contains(name) => BitPat(AluOp.ALU_OR)
      case name if aluXorInst.contains(name) => BitPat(AluOp.ALU_XOR)
      case name if aluSllInst.contains(name) => BitPat(AluOp.ALU_SLL)
      case name if aluSrlInst.contains(name) => BitPat(AluOp.ALU_SRL)
      case name if aluSraInst.contains(name) => BitPat(AluOp.ALU_SRA)
      case name if aluCmpInst.contains(name) => BitPat(AluOp.ALU_CMP)
      case name if aluCmpuInst.contains(name) => BitPat(AluOp.ALU_CMPU)
      case _ => BitPat(AluOp.ALU_ADD)
    }
  }
  def genCsrFunc(instPattern: InstructionPattern): BitPat = {
    instPattern.inst.name match {
      case name if csrWrtInst.contains(name) => BitPat(CSROp.CSR_WRT)
      case name if csrSetInst.contains(name) => BitPat(CSROp.CSR_SET)
      case name if csrMretInst.contains(name) => BitPat(CSROp.CSR_MRET)
      case name if csrEcallInst.contains(name) => BitPat(CSROp.CSR_ECALL)
      case _ => BitPat(CSROp.CSR_WRT)
    }
  }
  def genLsuFunc(instPattern: InstructionPattern): BitPat = {
    instPattern.inst.name match {
      case "lb" => BitPat(LSUOp.LSU_LB)
      case "lh" => BitPat(LSUOp.LSU_LH)
      case "lw" => BitPat(LSUOp.LSU_LW)
      case "lbu" => BitPat(LSUOp.LSU_LBU)
      case "lhu" => BitPat(LSUOp.LSU_LHU)
      case "sb" => BitPat(LSUOp.LSU_SB)
      case "sh" => BitPat(LSUOp.LSU_SH)
      case "sw" => BitPat(LSUOp.LSU_SW)
      case _ => BitPat(LSUOp.LSU_LB)
    }
  }
}

object BrTpe extends DecodeField[InstructionPattern, UInt] with HasCatgory{
  def name = "brtpe"
  def chiselType = UInt(2.W)
  def genTable(instPattern: InstructionPattern): BitPat = {
    genBrTpe(instPattern)
  }
  def genBrTpe(instPattern: InstructionPattern): BitPat = {
    instPattern.inst.name match {
      case name if brJInst.contains(name) => BitPat(BrOp.BR_J)
      case name if brBEInst.contains(name) => BitPat(BrOp.BR_BE)
      case name if brBGLInst.contains(name) => BitPat(BrOp.BR_GL)
      case _ => BitPat(BrOp.BR_XXX)
    }
  }
}

object Src1Tpe extends DecodeField[InstructionPattern, UInt] with HasCatgory{
  def name = "src1Tpe"
  def chiselType = UInt(1.W)
  def genTable(instPattern: InstructionPattern): BitPat = {
    if(instPattern.hasArg("rs1")) BitPat(Src1Type.rs1)
    else if(instPattern.inst.name == "lui") BitPat(Src1Type.rs1) //fix lui
    else BitPat(Src1Type.pc)
  }
}

object Src2Tpe extends DecodeField[InstructionPattern, UInt] with HasCatgory{
  def name = "src2Tpe"
  def chiselType = UInt(2.W)
  def genTable(instPattern: InstructionPattern): BitPat = {
    if(instPattern.hasArg("rs2")) BitPat(Src2Type.rs2)
    else if(instPattern.hasArg("csr")) BitPat(Src2Type.csrIdx) //fix csr
    else BitPat(Src2Type.imm)
  }
}

object RfWen extends BoolDecodeField[InstructionPattern]{
  def name = "rfWen"
  def genTable(instPattern: InstructionPattern): BitPat = {
    if(instPattern.hasArg("rd")) y else n
  }
}

object ImmTpe {
  val immNone = 0.U(3.W)
  val immI = 1.U(3.W)
  val immS = 2.U(3.W)
  val immB = 3.U(3.W)
  val immU = 4.U(3.W)
  val immJ = 5.U(3.W)
  def apply() = 3.W
}

object ImmType extends DecodeField[InstructionPattern, UInt] {
  override def name = "imm_type"
  override def chiselType = UInt(ImmTpe())
  override def genTable(i: InstructionPattern): BitPat = {
    val immType = i.inst.args
      .map(_.name match {
        case "imm12"                 => ImmTpe.immI
        case "imm12hi" | "imm12lo"   => ImmTpe.immS
        case "bimm12hi" | "bimm12lo" => ImmTpe.immB
        case "imm20"                 => ImmTpe.immU
        case "jimm20"                => ImmTpe.immJ
        case _                       => ImmTpe.immNone
      })
      .filterNot(_ == ImmTpe.immNone)
      .headOption // different ImmType will not appear in the Seq
      .getOrElse(ImmTpe.immNone)

    BitPat(immType)
  }
}

object AllFields{
  def apply() = Seq(
    ImmType, RfWen, Src2Tpe, Src1Tpe, BrTpe, Func, Fu
  )
}