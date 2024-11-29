package rtl

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.properties.{AnyClassType, Class, Property}
import chisel3.util.{DecoupledIO, Valid}
import chisel3.util.MuxLookup

object JKParameter {
  implicit def rwP: upickle.default.ReadWriter[JKParameter] =
    upickle.default.macroRW
}

/** Parameter of [[JK]] */
case class JKParameter(width: Int, useAsyncReset: Boolean) extends SerializableModuleParameter

/** Verification IO of [[JK]] */
class JKProbe(parameter: JKParameter) extends Bundle {
  val jkReg = Bool()
}

/** Metadata of [[JK]]. */
@instantiable
class JKOM(parameter: JKParameter) extends Class {
  val width:         Property[Int]     = IO(Output(Property[Int]()))
  val useAsyncReset: Property[Boolean] = IO(Output(Property[Boolean]()))
  width         := Property(parameter.width)
  useAsyncReset := Property(parameter.useAsyncReset)
}

/** Interface of [[JK]]. */
class JKInterface(parameter: JKParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val input  = Input(new Bundle {
    val j = UInt(parameter.width.W)
    val k = UInt(parameter.width.W)
  })
  val output = Output(new Bundle{
    val q = UInt(parameter.width.W)
    val q1 = UInt(parameter.width.W)
  })
  val probe  = Output(Probe(new JKProbe(parameter)))
  val om     = Output(Property[AnyClassType]())
}

/** Hardware Implementation of JK */
@instantiable
class JK(val parameter: JKParameter)
    extends FixedIORawModule(new JKInterface(parameter))
    with SerializableModule[JKParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val jkReg = Reg(UInt(parameter.width.W))
  when(io.input.j === 1.U && io.input.k === 0.U){
    jkReg := 1.U
  }.elsewhen(io.input.j === 0.U && io.input.k === 1.U){
    jkReg := 0.U
  }.elsewhen(io.input.j === 1.U && io.input.k === 1.U){
    jkReg := ~jkReg
  }
  io.output.q := jkReg
  io.output.q1 := ~jkReg

  // Assign Probe
  val probeWire: JKProbe = Wire(new JKProbe(parameter))
  define(io.probe, ProbeValue(probeWire))
  probeWire.jkReg := jkReg

  // Assign Metadata
  val omInstance: Instance[JKOM] = Instantiate(new JKOM(parameter))
  io.om := omInstance.getPropertyReference.asAnyClassType
}
