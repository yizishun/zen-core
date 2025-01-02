package tb

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.ltl.Property.{eventually, not}
import chisel3.ltl.{AssertProperty, CoverProperty, Delay, Sequence}
import chisel3.properties.{AnyClassType, Class, Property}
import chisel3.util.circt.dpi.{RawClockedNonVoidFunctionCall, RawUnclockedNonVoidFunctionCall}
import chisel3.util.{Counter, HasExtModuleInline, RegEnable, Valid}
import chisel3.layers.Verification.Assume
import chisel3.ltl.AssumeProperty
import zen.backend.fu.{ALU, ALUParameter, AluOp}
import zen._

object ALUFormalParameter {
  implicit def rwP: upickle.default.ReadWriter[ALUFormalParameter] =
    upickle.default.macroRW
}

/** Parameter of [[ALU]]. */
case class ALUFormalParameter(aluParameter: ALUParameter, isSBY: Boolean, isVCF: Boolean) extends SerializableModuleParameter {}

class ALUFormalInterface(parameter: ALUFormalParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.aluParameter.useAsyncReset) AsyncReset() else Bool())
  val src = Vec(2, Input(UInt(parameter.aluParameter.width.W)))
  val func = Input(UInt(4.W))
  val valid = Input(Bool())
  val ready = Output(Bool())
}

@instantiable
class ALUFormal(val parameter: ALUFormalParameter)
    extends FixedIORawModule(new ALUFormalInterface(parameter))
    with SerializableModule[ALUFormalParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock         = io.clock
  override protected def implicitReset: Reset         = io.reset
  // Instantiate DUT.
  val dut: Instance[ALU] = Instantiate(new ALU(parameter.aluParameter))
  dut.io.clock := implicitClock
  dut.io.reset := implicitReset
  dut.io.out.ready := true.B

  // Connect DUT inputs
  dut.io.in.valid := io.valid
  dut.io.in.bits.src(0) := io.src(0)
  dut.io.in.bits.src(1) := io.src(1)
  dut.io.in.bits.func := io.func
  io.ready := dut.io.in.ready

  if (parameter.isSBY) {
    import Sequence._
    val addCorrect: Sequence = dut.io.out.bits === (io.src(0) + io.src(1))
    val subCorrect: Sequence = dut.io.out.bits === (io.src(0) - io.src(1))
    val sllCorrect: Sequence = dut.io.out.bits === (io.src(0) << io.src(1)(4, 0))(31, 0)
    val isADD: Sequence = io.func === "b1000".U
    val isSLL: Sequence = io.func === "b0111".U

    AssumeProperty(
      isSLL,
      clock = None,
      disable = None
    )

    AssertProperty(
      sllCorrect,
      clock= None,
      disable = None
    )

  } else if(parameter.isVCF) {
    import Sequence._

    val isAdd:  Sequence = io.func === "b1000".U  // ALU_ADD
    val isSub:  Sequence = io.func === "b1001".U  // ALU_SUB
    val isAnd:  Sequence = io.func === "b0000".U  // ALU_AND
    val isCmp:  Sequence = io.func === "b0001".U  // ALU_CMP
    val isXor:  Sequence = io.func === "b0010".U  // ALU_XOR
    val isCmpu: Sequence = io.func === "b0011".U  // ALU_CMPU
    val isOr:   Sequence = io.func === "b0100".U  // ALU_OR
    val isSra:  Sequence = io.func === "b0101".U  // ALU_SRA
    val isSrl:  Sequence = io.func === "b0110".U  // ALU_SRL
    val isSll:  Sequence = io.func === "b0111".U  // ALU_SLL

    // 加法结果断言
    AssertProperty(
      isAdd |-> (dut.io.out.bits === (io.src(0) + io.src(1))),
      label = Some("ALU_RESULT_ADD_CORRECT")
    )

    // 减法结果断言
    AssertProperty(
      isSub |-> (dut.io.out.bits === (io.src(0) - io.src(1))),
      label = Some("ALU_RESULT_SUB_CORRECT")
    )

    // 按位与结果断言
    AssertProperty(
      isAnd |-> (dut.io.out.bits === (io.src(0) & io.src(1))),
      label = Some("ALU_RESULT_AND_CORRECT")
    )

    // 有符号比较结果断言
    AssertProperty(
      isCmp |-> (dut.io.out.bits === (io.src(0).asSInt < io.src(1).asSInt).asUInt),
      label = Some("ALU_RESULT_CMP_CORRECT")
    )

    // 按位异或结果断言
    AssertProperty(
      isXor |-> (dut.io.out.bits === (io.src(0) ^ io.src(1))),
      label = Some("ALU_RESULT_XOR_CORRECT")
    )

    // 无符号比较结果断言
    AssertProperty(
      isCmpu |-> (dut.io.out.bits === (io.src(0) < io.src(1))),
      label = Some("ALU_RESULT_CMPU_CORRECT")
    )

    // 按位或结果断言
    AssertProperty(
      isOr |-> (dut.io.out.bits === (io.src(0) | io.src(1))),
      label = Some("ALU_RESULT_OR_CORRECT")
    )

    // 算术右移结果断言
    AssertProperty(
      isSra |-> (dut.io.out.bits === (io.src(0).asSInt >> io.src(1)(4, 0)).asUInt),
      label = Some("ALU_RESULT_SRA_CORRECT")
    )

    // 逻辑右移结果断言
    AssertProperty(
      isSrl |-> (dut.io.out.bits === (io.src(0) >> io.src(1)(4, 0))),
      label = Some("ALU_RESULT_SRL_CORRECT")
    )

    // 逻辑左移结果断言
    AssertProperty(
      isSll |-> (dut.io.out.bits === (io.src(0) << io.src(1)(4, 0))(31, 0)),
      label = Some("ALU_RESULT_SLL_CORRECT")
    )
  }
}
