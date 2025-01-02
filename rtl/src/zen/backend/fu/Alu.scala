package zen.backend.fu

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.util.{DecoupledIO, Valid, MuxLookup, Fill}
import utils.InsertReg._
import zen._

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

object AluOp {
  def ALU_AND  = "b0000".U(4.W)  // AND
  def ALU_CMP  = "b0001".U(4.W)  // CMP
  def ALU_XOR  = "b0010".U(4.W)  // XOR
  def ALU_CMPU = "b0011".U(4.W)  // CMPU
  def ALU_OR   = "b0100".U(4.W)  // OR
  def ALU_SRA  = "b0101".U(4.W)  // SRA
  def ALU_SRL  = "b0110".U(4.W)  // SRL
  def ALU_SLL  = "b0111".U(4.W)  // SLL
  def ALU_ADD  = "b1000".U(4.W)  // ADD
  def ALU_SUB  = "b1001".U(4.W)  // SUB

  def isAdd1(func: UInt): Bool = func(0)
  def isAddSub(func: UInt): Bool = func(3)
}

/** Interface of [[ALU]]. */
class ALUInterface(parameter: ALUParameter) extends FunctionIO(parameter.width, 4) {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
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

  val src1 = io.in.bits.src(0).withReg(parameter.analysisTiming)
  val src2 = io.in.bits.src(1).withReg(parameter.analysisTiming)
  val func = io.in.bits.func.withReg(parameter.analysisTiming)

  def access(valid: Bool, src1: UInt, src2: UInt, func: UInt): UInt = {
    this.io.in.valid := valid
    this.io.in.bits.src(0) := src1
    this.io.in.bits.src(1) := src2
    this.io.in.bits.func := func
    io.out.bits
  }

  val shamt = src2(4, 0).asUInt
  val isAdderSub = AluOp.isAdd1(func)
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
  val res1 = MuxLookup(func(2, 0), reAnd)(
    Seq(
      ALU_OR(2,0)  -> reOr,
      ALU_XOR(2,0) -> reXor,
      ALU_SRA(2,0) -> reSra,
      ALU_SRL(2,0) -> reSrl,
      ALU_SLL(2,0) -> reSll
    )
  )
  io.out.bits := Mux(isAddSub(func), reAddSub, 
                 Mux(func === ALU_CMPU, reCmpu, 
                 Mux(func === ALU_CMP, reCmp, res1))).withReg(parameter.analysisTiming)
  io.Zf.foreach(_ := (io.out.bits === 0.U).withReg(parameter.analysisTiming))
  io.Nf.foreach(_ := io.out.bits(parameter.width - 1).withReg(parameter.analysisTiming))

  // Handshake
  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
