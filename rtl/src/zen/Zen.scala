package zen

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util._
import chisel3.probe._
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
  val imemOut = new SimpleBus()
  val ifuProbe = new Bundle {
    val out_ready = RWProbe(Bool())
  }
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
  ifu.io.isFlush := false.B
  ifu.io.correctedPC := 0.U

  // 实例化 ICache
  val icache = Instantiate(new ICache()(parameter.icacheParameter))
  icache.io.fencei <> DontCare
  icache.io.clock := io.clock
  icache.io.reset := io.reset
  icache.io.out <> io.imemOut

  // 连接 IFU 和 ICache
  ifu.io.imem <> icache.io.in

  val ifu_out_ready = Wire(Bool())
  ifu_out_ready := false.B
  define(io.ifuProbe.out_ready, RWProbeValue(ifu_out_ready))
  ifu.io.out.ready := ifu_out_ready

} 