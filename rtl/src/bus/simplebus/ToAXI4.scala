/**************************************************************************************
* Copyright (c) 2020 Institute of Computing Technology, CAS
* Copyright (c) 2020 University of Chinese Academy of Sciences
*
* NutShell is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*             http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR
* FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

package bus.simplebus

import chisel3._
import chisel3.util._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import utils._
import bus._
import bus.axi4._

object SimpleBus2AXI4ConverterParameter {
  implicit def rwP: upickle.default.ReadWriter[SimpleBus2AXI4ConverterParameter] =
    upickle.default.macroRW
}

case class SimpleBus2AXI4ConverterParameter(
  useAsyncReset: Boolean = false,
  lineBeats: Int = 4
) extends SerializableModuleParameter

class SimpleBus2AXI4ConverterInterface(parameter: SimpleBus2AXI4ConverterParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val in = Flipped(new SimpleBus)
  val out = Flipped(Flipped(AXI4()))
}

//area: 210.938000
//timing: 2702.301MHZ
@instantiable
class SimpleBus2AXI4Converter(val parameter: SimpleBus2AXI4ConverterParameter)
    extends FixedIORawModule(new SimpleBus2AXI4ConverterInterface(parameter))
    with SerializableModule[SimpleBus2AXI4ConverterParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  val (mem, axi) = (io.in, io.out)
  val (ar, aw, w, r, b) = (axi.ar.bits, axi.aw.bits, axi.w.bits, axi.r.bits, axi.b.bits)

  // req payload
  ar.addr  := mem.req.bits.addr
  w.data := mem.req.bits.wdata
  w.strb := mem.req.bits.wmask

  // req beat control
  val wlast = WireInit(true.B)
  val rlast = WireInit(true.B)
  axi.ar.bits.id    := 0.U
  axi.ar.bits.len   := Mux(mem.req.bits.isBurst(), (parameter.lineBeats - 1).U, 0.U)
  axi.ar.bits.size  := mem.req.bits.size
  axi.ar.bits.burst := AXI4Parameters.BURST_INCR
  axi.w.bits.last   := mem.req.bits.isWriteLast() || mem.req.bits.isWriteSingle()
  wlast := axi.w.bits.last
  rlast := axi.r.bits.last

  aw := ar
  // res payload
  mem.resp.bits.rdata := r.data
  mem.resp.bits.cmd  := Mux(rlast, SimpleBusCmd.readLast, 0.U)

  // handshake
  val wSend = Wire(Bool())
  val awAck = BoolStopWatch(axi.aw.fire, wSend)
  val wAck = BoolStopWatch(axi.w.fire && wlast, wSend)
  wSend := (axi.aw.fire && axi.w.fire && wlast) || (awAck && wAck)
  val wen = RegEnable(mem.req.bits.isWrite(), mem.req.fire)

  axi.ar.valid := mem.isRead
  axi.aw.valid := mem.isWrite && !awAck
  axi.w .valid := mem.isWrite && !wAck
  mem.req.ready  := Mux(mem.req.bits.isWrite(), !wAck && axi.w.ready, axi.ar.ready)

  axi.r.ready  := mem.resp.ready
  axi.b.ready  := mem.resp.ready
  mem.resp.valid  := Mux(wen, axi.b.valid, axi.r.valid)
}

object SimpleBus2AXI4Converter {
  def apply(in: SimpleBus): AXI4 = {
    val bridge = Module(new SimpleBus2AXI4Converter(SimpleBus2AXI4ConverterParameter()))
    bridge.io.in <> in
    bridge.io.out
  }
}
