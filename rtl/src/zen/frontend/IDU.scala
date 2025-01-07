package zen.frontend

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.properties.{AnyClassType, Class, Property}
import chisel3.util._
import zen._
import utils._

object IDUParameter {
  implicit def rwP: upickle.default.ReadWriter[IDUParameter] =
    upickle.default.macroRW
}

/** Parameter of [[IDU]] */
case class IDUParameter(width: Int, useAsyncReset: Boolean) extends SerializableModuleParameter

/** Interface of [[IDU]]. */
class IDUInterface(parameter: IDUParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
}

/** Hardware Implementation of IDU */
@instantiable
class IDU(val parameter: IDUParameter)
    extends FixedIORawModule(new IDUInterface(parameter))
    with SerializableModule[IDUParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

}
