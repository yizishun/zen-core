package zen.backend.fu

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate, Definition}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.util._
import zen._
import utils._

object CSRParameter {
  implicit def rwP: upickle.default.ReadWriter[CSRParameter] =
    upickle.default.macroRW
}

/** Parameter of [[CSR]] */
case class CSRParameter(width: Int, useAsyncReset: Boolean) extends SerializableModuleParameter

object CSROp {
  def CSR_WRT = 0.U(1.W)
  def CSR_SET = 1.U(1.W)
}

trait HasCSRCons {
  val Mstatus = 0x300
  val Mtvec = 0x305
  val Mepc = 0x341
  val Mcause = 0x342
  val Mvendorid = 0xF11
  val Marchid = 0xF12
}

/** Interface of [[CSR]]. */
class CSRInterface(parameter: CSRParameter) extends FunctionIO(parameter.width, CSROp.CSR_WRT.getWidth) {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
}

//TODO: 异常支持
/** Hardware Implementation of CSR */
@instantiable
class CSR(val parameter: CSRParameter)
    extends FixedIORawModule(new CSRInterface(parameter))
    with SerializableModule[CSRParameter]
    with ImplicitClock
    with ImplicitReset
    with HasCSRCons {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val (func, src1, csrAddr) = (io.in.bits.func, io.in.bits.src(0), io.in.bits.src(1)(11, 0))

  val mstatus = RegInit(0x1800.U(parameter.width.W))
  val mtvec = RegInit(0.U(parameter.width.W))
  val mepc = RegInit(0.U(parameter.width.W))
  val mcause = RegInit(0.U(parameter.width.W))
  val mvendorid = RegInit("h79737978".U(parameter.width.W))
  val marchid = RegInit(23060171.U(parameter.width.W))
  val mapping = Map(
    RegMap(Mstatus, mstatus),
    RegMap(Mtvec, mtvec),
    RegMap(Mepc, mepc),
    RegMap(Mcause, mcause),
    RegMap(Mvendorid, mvendorid),
    RegMap(Marchid, marchid)
  )
  val rdata = Wire(UInt(parameter.width.W))
  val wdata = Mux(func === CSROp.CSR_SET, src1 | rdata, src1)

  RegMap.generate(mapping, csrAddr, rdata, csrAddr, io.in.valid, wdata)
  io.in.ready := true.B
  io.out.valid := io.in.valid

  io.out.bits := rdata

}

object CSR {
  def apply(parameter: CSRParameter, clock: Clock, reset: Reset, valid: Bool, src1: UInt, src2: UInt, func: UInt): Instance[CSR] = {
    val csr = Instantiate(new CSR(parameter))
    csr.io.clock := clock
    csr.io.reset := reset  
    csr.io.in.valid := valid
    csr.io.in.bits.src(0) := src1
    csr.io.in.bits.src(1) := src2
    csr.io.in.bits.func := func
    csr
  }
}
