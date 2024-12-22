package tb

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.ltl.Property.{eventually, not}
import chisel3.ltl.{AssertProperty, CoverProperty, Delay, Sequence}
import chisel3.properties.{AnyClassType, Class, Property}
import chisel3.util.circt.dpi.{RawClockedNonVoidFunctionCall, RawUnclockedNonVoidFunctionCall}
import chisel3.util.{Counter, HasExtModuleInline, RegEnable, Valid, Decoupled}
import chisel3.layers.Verification.Assume
import chisel3.ltl.AssumeProperty
import zen.frontend._
import bus.simplebus._
import zen._
import java.awt.Label

object IFUFormalParameter {
  implicit def rwP: upickle.default.ReadWriter[IFUFormalParameter] =
    upickle.default.macroRW
}

/** Parameter of [[IFU]]. */
case class IFUFormalParameter(ifuParameter: IFUParameter) extends SerializableModuleParameter {}

class IFUFormalInterface(parameter: IFUFormalParameter) extends Bundle {
  val clock = Input(Clock())
  val reset = Input(if (parameter.ifuParameter.useAsyncReset) AsyncReset() else Bool())
  val imem = new SimpleBus()
  val isFlush = Input(Bool())
  val correctedPC = Input(UInt(parameter.ifuParameter.width.W))
  val out = Decoupled(new IFUOutIO(parameter.ifuParameter.width))
}

@instantiable
class IFUFormal(val parameter: IFUFormalParameter)
    extends FixedIORawModule(new IFUFormalInterface(parameter))
    with SerializableModule[IFUFormalParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock         = io.clock
  override protected def implicitReset: Reset         = io.reset
  // Instantiate DUT.
  val dut:                              Instance[IFU] = Instantiate(new IFU(parameter.ifuParameter))

  dut.io.clock := implicitClock
  dut.io.reset := implicitReset

  // LTL Checker
  import Sequence._
  val imemReqFire:     Sequence = dut.io.imem.req.fire
  val imemReqNotValid: Sequence = !dut.io.imem.req.valid && dut.io.imem.req.ready
  val imemReqNotFire:  Sequence = !dut.io.imem.req.fire
  val imemRespFire:    Sequence = dut.io.imem.resp.valid && dut.io.imem.resp.ready
  val imemRespNotFire: Sequence = !dut.io.imem.resp.valid || !dut.io.imem.resp.ready

  dut.io.imem <> io.imem
  dut.io.isFlush := io.isFlush
  dut.io.correctedPC := io.correctedPC
  io.out <> dut.io.out

  AssumeProperty(
    imemReqNotValid |=> not(imemReqFire),
    label = Some("IFU_ASSUMPTION_INPUT_NOT_VALID")
  )

  AssertProperty(
    imemReqFire |=> imemReqNotFire.repeatAtLeast(1) ### imemRespFire,
    label = Some("IFU_ALWAYS_RESPONSE")
  )
  AssertProperty(
    imemReqFire |-> not(imemReqNotFire.repeatAtLeast(1) ### imemRespNotFire.and(imemReqFire)),
    label = Some("GCD_NO_DOUBLE_FIRE") 
  )

}
