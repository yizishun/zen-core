package zen.backend.fu

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.util.{DecoupledIO, Valid, MuxLookup}
import utils.InsertReg._

object ALUParameter {
  implicit def rwP: upickle.default.ReadWriter[ALUParameter] =
    upickle.default.macroRW
}

/** Parameter of [[ALU]] */
case class ALUParameter(
  width: Int, 
  useAsyncReset: Boolean,
  hasOf: Boolean,
  hasZf: Boolean,
  hasNf: Boolean,
  hasCf: Boolean,
  analysisTiming: Boolean
  ) extends SerializableModuleParameter

object AluOp extends ChiselEnum {
  val ALU_ADD,
      ALU_SUB,
      ALU_AND,
      ALU_CMP,
      ALU_XOR,
      ALU_CMPU,
      ALU_OR,
      ALU_SRA,
      ALU_SRL,
      ALU_SLL = Value
}

/** Interface of [[ALU]]. */
class ALUInterface(parameter: ALUParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val src = Vec(2, Input(UInt(parameter.width.W)))
  val func = Input(AluOp())
  val result = Output(UInt(parameter.width.W))
  val Of: Option[Bool] = if (parameter.hasOf) Some(Output(Bool())) else None
  val Zf: Option[Bool] = if (parameter.hasZf) Some(Output(Bool())) else None
  val Nf: Option[Bool] = if (parameter.hasNf) Some(Output(Bool())) else None
  val Cf: Option[Bool] = if (parameter.hasCf) Some(Output(Bool())) else None
}

/** Hardware Implementation of ALU */
@instantiable
class ALU(val parameter: ALUParameter)
    extends FixedIORawModule(new ALUInterface(parameter))
    with SerializableModule[ALUParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val src1 = io.src(0).withReg(parameter.analysisTiming)
  val src2 = io.src(1).withReg(parameter.analysisTiming)
  val func = io.func.withReg(parameter.analysisTiming)

  val shamt = src2(4, 0).asUInt
  val reAddSub = src1 +& Mux((func.asUInt)(0), -src2, src2)
  val reAnd = src1 & src2
  val reOr = src1 | src2
  val reXor = src1 ^ src2
  val reCmp = src1.asSInt < src2.asSInt
  val reCmpu = src1 < src2
  val reSra = (src1.asSInt >> shamt).asUInt
  val reSrl = src1 >> shamt
  val reSll = src1 << shamt

  import AluOp._
  io.Cf.foreach(_ := reAddSub(parameter.width).withReg(parameter.analysisTiming))
  io.Of.foreach(_ := reAddSub(parameter.width).withReg(parameter.analysisTiming))
  io.result := MuxLookup(func, src2)(
    Seq(
      ALU_ADD -> reAddSub,
      ALU_SUB -> reAddSub,
      ALU_AND -> reAnd,
      ALU_OR  -> reOr,
      ALU_XOR -> reXor,
      ALU_CMP -> reCmp,
      ALU_CMPU-> reCmpu,
      ALU_SRA -> reSra,
      ALU_SRL -> reSrl,
      ALU_SLL -> reSll
    )
  ).withReg(parameter.analysisTiming)
  io.Zf.foreach(_ := (io.result === 0.U).withReg(parameter.analysisTiming))
  io.Nf.foreach(_ := io.result(parameter.width - 1).withReg(parameter.analysisTiming))


}
