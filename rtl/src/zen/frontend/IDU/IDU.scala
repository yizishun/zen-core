package zen.frontend

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.properties.{AnyClassType, Class, Property}
import chisel3.util._
import chisel3.util.experimental.decode._
import zen._
import utils._

object IDUParameter {
  implicit def rwP: upickle.default.ReadWriter[IDUParameter] =
    upickle.default.macroRW
}

/** Parameter of [[IDU]] */
case class IDUParameter(
  width: Int,
  useAsyncReset: Boolean,
) extends SerializableModuleParameter

/** Interface of [[IDU]]. */
class IDUInterface(parameter: IDUParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val in = Flipped(Decoupled(new IFUOutIO(parameter.width)))
  val out = Decoupled(new DecodeIO(parameter.width))
  val isFlush = Input(Bool())
}

//TODO: Flush fence.i
/** Hardware Implementation of [[IDU]] */
@instantiable
class IDU(val parameter: IDUParameter)
    extends FixedIORawModule(new IDUInterface(parameter))
    with SerializableModule[IDUParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  /**
    * 1. decode
    * 2. 发送control signals
    * 3. 发送data（pc,imm,csrIdx）
    */
  val (inst, pc) = (io.in.bits.inst, io.in.bits.pc)
  val possibleInput = PossibleInput(os.pwd / "dependencies" / "rvdecoderdb" / "rvdecoderdbtest" / "jvm" / "riscv-opcodes")
  //val possibleInput = PossibleInput(os.pwd / "rv_op")
  //possibleInput.map(_.toString).foreach(println) //可以打印出所有输入的指令信息
  val allFields = AllFields()

  //decode
  val decodeTable = new DecodeTable(possibleInput, allFields)
  val decodeResult = decodeTable.decode(inst)

  //control signals
  io.out.bits.control.send(
    fu = decodeResult(Fu),
    func = decodeResult(Func),
    brTpe = decodeResult(BrTpe),
    src1Tpe = decodeResult(Src1Tpe),
    src2Tpe = decodeResult(Src2Tpe),
    rfSrc1 = inst(19, 15),
    rfSrc2 = inst(24, 20),
    rfDest = inst(11, 7),
    rfWen = decodeResult(RfWen)
  )

  //imm
  val immTpe = decodeResult(ImmType)
  val immI = inst(31, 20).asSInt
  val immU = Cat(inst(31, 12), 0.U(12.W)).asSInt
  val immJ = Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)).asSInt
  val immS = Cat(inst(31, 25), inst(11, 7)).asSInt
  val immB = Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)).asSInt
  io.out.bits.imm := MuxLookup(immTpe, 0.S)(Seq(
    ImmTpe.immI -> immI,
    ImmTpe.immU -> immU,
    ImmTpe.immJ -> immJ,
    ImmTpe.immS -> immS,
    ImmTpe.immB -> immB
  )).asUInt

  //pc
  io.out.bits.pc := pc

  //csrIdx
  io.out.bits.csrIdx := inst(31, 20)

  //handshake
  io.out.valid := io.in.valid
  io.in.ready := !io.in.valid || io.out.fire

}
