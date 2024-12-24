package zen

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util._
import utils._
import bus.simplebus._
import zen.frontend._

object ZenParameter {
  implicit def rwP: upickle.default.ReadWriter[ZenParameter] =
    upickle.default.macroRW
}

/** Parameter of [[Zen]] */
case class ZenParameter(
  ifuParameter: IFUParameter,
  icacheParameter: ICacheParameter
) extends SerializableModuleParameter

/** Interface of [[Zen]]. */
class ZenInterface(parameter: ZenParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.ifuParameter.useAsyncReset) AsyncReset() else Bool())
  val out = Decoupled(new IFUOutIO(parameter.ifuParameter.width))
  val isFlush = Input(Bool())
  val correctedPC = Input(UInt(parameter.ifuParameter.width.W))
  val fencei = Flipped(ValidIO(new Bundle {
    val is_fencei = Bool()
  }))
}

/** Hardware Implementation of [[Zen]] */
@instantiable
class Zen(val parameter: ZenParameter)
    extends FixedIORawModule(new ZenInterface(parameter))
    with SerializableModule[ZenParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = s"Zen"

  // 实例化 IFU
  val ifu = Instantiate(new IFU(parameter.ifuParameter))
  ifu.io.clock := io.clock
  ifu.io.reset := io.reset
  ifu.io.isFlush := io.isFlush
  ifu.io.correctedPC := io.correctedPC
  io.out <> ifu.io.out

  // 实例化 ICache
  val icache = Instantiate(new ICache()(parameter.icacheParameter))
  icache.io <> DontCare
  icache.io.clock := io.clock
  icache.io.reset := io.reset
  icache.io.fencei <> io.fencei

  // 连接 IFU 和 ICache
  ifu.io.imem <> icache.io.in

} 