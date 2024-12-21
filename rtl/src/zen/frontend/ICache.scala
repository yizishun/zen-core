package zen.frontend

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.util.{DecoupledIO, Valid, MuxLookup, Fill}
import utils.InsertReg._

object ICacheParameter {
  implicit def rwP: upickle.default.ReadWriter[ICacheParameter] =
    upickle.default.macroRW
}

/** Parameter of [[ICache]] */
case class ICacheParameter(
  width: Int, 
  useAsyncReset: Boolean,
  analysisTiming: Boolean
) extends SerializableModuleParameter

/** Interface of [[ICache]]. */
class ICacheInterface(parameter: ICacheParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
}

/** Hardware Implementation of ICache */
//yosys:
//area = 1602.384000
//timing = 1248.974MHZ
@instantiable
class ICache(val parameter: ICacheParameter)
    extends FixedIORawModule(new ICacheInterface(parameter))
    with SerializableModule[ICacheParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = s"ICache"
}
