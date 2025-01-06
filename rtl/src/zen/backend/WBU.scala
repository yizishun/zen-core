package zen.backend

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.util._
import zen._

object WBUParameter {
  implicit def rwP: upickle.default.ReadWriter[WBUParameter] =
    upickle.default.macroRW
}

/** Parameter of [[WBU]] */
case class WBUParameter(width: Int, useAsyncReset: Boolean) extends SerializableModuleParameter


/** Interface of [[WBU]]. */
class WBUInterface(parameter: WBUParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val in = Input(Decoupled(new WriteBackIO(parameter.width)))
  val wb = Output(new WriteBackIO(parameter.width))

}

/** Hardware Implementation of WBU */
@instantiable
class WBU(val parameter: WBUParameter)
    extends FixedIORawModule(new WBUInterface(parameter))
    with SerializableModule[WBUParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  io.in.ready := true.B
  io.wb := io.in.bits

}
