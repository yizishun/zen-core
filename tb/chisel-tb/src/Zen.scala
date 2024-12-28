package tb

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.ltl.Property.{eventually, not}
import chisel3.ltl.{AssertProperty, CoverProperty, Delay, Sequence}
import chisel3.properties.{AnyClassType, Class, Property}
import chisel3.util.circt.dpi.{RawClockedNonVoidFunctionCall, RawUnclockedNonVoidFunctionCall}
import chisel3.util._
import chisel3.probe._
import rtl._
import zen._
import zen.frontend._
import bus.simplebus._
import utils._

object ZenTestBenchParameter {
  implicit def rwP: upickle.default.ReadWriter[ZenTestBenchParameter] =
    upickle.default.macroRW
}

/** Parameter of [[ZenTestBench]]. */
case class ZenTestBenchParameter(
  timeout: Int,
  testVerbatimParameter: TestVerbatimParameter,
  zenParameter: ZenParameter)
    extends SerializableModuleParameter {
  require(
    (testVerbatimParameter.useAsyncReset && zenParameter.ifuParameter.useAsyncReset) ||
      (!testVerbatimParameter.useAsyncReset && !zenParameter.ifuParameter.useAsyncReset),
    "Reset Type check failed."
  )
}

class ZenTestBenchInterface(parameter: ZenTestBenchParameter) extends Bundle {
}

@instantiable
class ZenTestBench(val parameter: ZenTestBenchParameter)
    extends FixedIORawModule(new ZenTestBenchInterface(parameter))
    with SerializableModule[ZenTestBenchParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = verbatim.io.clock
  override protected def implicitReset: Reset = verbatim.io.reset
  override val desiredName: String = "TB"

  // Instantiate Drivers
  val verbatim: Instance[TestVerbatim] = Instantiate(
    new TestVerbatim(parameter.testVerbatimParameter)
  )
  val dut: Instance[Zen] = Instantiate(new Zen(parameter.zenParameter))

  val imem = Wire(Flipped(new SimpleBus()))
  dut.io.imemOut <> imem
  // Instantiate DUT
  dut.io.clock := implicitClock
  dut.io.reset := implicitReset

  // Simulation Logic
  val simulationTime: UInt = RegInit(0.U(64.W))
  simulationTime := simulationTime + 1.U
  val (_, callWatchdog) = Counter(true.B, parameter.timeout / 2)
  val watchdogCode      = RawUnclockedNonVoidFunctionCall("zen_watchdog", UInt(8.W))(callWatchdog)
  when(watchdogCode =/= 0.U) {
    stop(cf"""{"event":"SimulationStop","reason": ${watchdogCode},"cycle":${simulationTime}}\n""")
  }
  imem.req.ready := false.B
  imem.resp.valid := false.B
  imem.resp.bits.rdata := 0.U
  imem.resp.bits.cmd := 0.U


  when(simulationTime > 10.U) {
    //start simulation
    val handshake = Wire(Vec(2, Bool()))
    val finish = handshake(1)
    val resetHandshake = WireInit(Bool(), finish)
    handshake(0) := Handshake.slave(imem.req, true.B, resetHandshake)
    handshake(1) := Handshake.simpleBusMaster(imem.resp, RegNext(handshake(0)), resetHandshake, burst = true, burstLen = 2)
    imem.resp.bits.rdata := simulationTime(parameter.zenParameter.ifuParameter.width - 1, 0).asUInt
  }
  force(dut.io.ifuProbe.out_ready, true.B)

}

