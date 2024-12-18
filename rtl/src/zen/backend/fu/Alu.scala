package zen.backend.fu

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.util.{DecoupledIO, Valid, MuxLookup, Fill}
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
  val 
      ALU_AND, // 0000
      ALU_CMP, // 0001
      ALU_XOR, // 0010
      ALU_CMPU,// 0011
      ALU_OR,  // 0100
      ALU_SRA, // 0101
      ALU_SRL, // 0110
      ALU_SLL, // 0111
      ALU_ADD, // 1000
      ALU_SUB = Value //1001
  def isAdd1[T <: EnumType](func: T): Bool = (func.asUInt)(0)
  def isAddSub[T <: EnumType](func: T): Bool = (func.asUInt)(3)
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
//yosys:
//area = 1602.384000
//timing = 1248.974MHZ
@instantiable
class ALU(val parameter: ALUParameter)
    extends FixedIORawModule(new ALUInterface(parameter))
    with SerializableModule[ALUParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = s"ALU"
  import AluOp._

  val src1 = io.src(0).withReg(parameter.analysisTiming)
  val src2 = io.src(1).withReg(parameter.analysisTiming)
  val func = io.func.withReg(parameter.analysisTiming)

  val shamt = src2(4, 0).asUInt
  val isAdderSub = AluOp.isAdd1(io.func)
  val reAddSub = (src1 +& (src2 ^ Fill(parameter.width, isAdderSub))) + isAdderSub
  val reCmpu = !reAddSub(parameter.width.U)
  val reXor = src1 ^ src2
  val reCmp = reXor(parameter.width.U - 1.U) ^ reCmpu
  val reAnd = src1 & src2
  val reOr = src1 | src2
  val reSra = (src1.asSInt >> shamt).asUInt
  val reSrl = src1 >> shamt
  val reSll = src1 << shamt

  io.Cf.foreach(_ := reAddSub(parameter.width).withReg(parameter.analysisTiming))
  io.Of.foreach(_ := reAddSub(parameter.width).withReg(parameter.analysisTiming))
  val res1 = MuxLookup(AluOp((func.asUInt)(2, 0)), reAnd)(
    Seq(
      ALU_OR  -> reOr,
      ALU_XOR -> reXor,
      ALU_SRA -> reSra,
      ALU_SRL -> reSrl,
      ALU_SLL -> reSll,
    )
  )
  io.result := Mux(isAddSub(io.func), reAddSub, 
               Mux(io.func === ALU_CMPU, reCmpu, 
               Mux(io.func === ALU_CMP , reCmp, res1))).withReg(parameter.analysisTiming)
  io.Zf.foreach(_ := (io.result === 0.U).withReg(parameter.analysisTiming))
  io.Nf.foreach(_ := io.result(parameter.width - 1).withReg(parameter.analysisTiming))
}
