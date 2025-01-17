package zen.frontend

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe._
import chisel3.util._
import utils._
import bus.simplebus._
import zen._

object IFUParameter {
  implicit def rwP: upickle.default.ReadWriter[IFUParameter] =
    upickle.default.macroRW
}

/** Parameter of [[IFU]] */
case class IFUParameter(
  width: Int,
  useAsyncReset: Boolean,
  resetPC: String,
  usePerformanceProbe: Boolean
) extends SerializableModuleParameter

/** Interface of [[IFU]]. */
class IFUInterface(parameter: IFUParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val out = Decoupled(new IFUOutIO(parameter.width))
  val imem = new SimpleBus()
  val isFlush = Output(Vec(4, Bool()))
  val correctedPC = Flipped(Valid(new TargetPC(parameter.width)))
}

/** Hardware Implementation of [[IFU]] */
//yosys:
//area: 644.518000
//timing: 1938.033MHZ 
@instantiable
class IFU(val parameter: IFUParameter)
    extends FixedIORawModule(new IFUInterface(parameter))
    with SerializableModule[IFUParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = s"IFU"

  dontTouch(io.out)
  dontTouch(io.imem)
  
  // 将十六进制字符串转换为BigInt，然后转为UInt
  val resetPCValue = BigInt(parameter.resetPC.stripPrefix("0x"), 16)
  val pcReg = RegInit(resetPCValue.U(32.W))
  val snpc = pcReg + 4.U // for future, use branch predictor
  val npc = Mux(io.correctedPC.valid, io.correctedPC.bits.pc, snpc)

  // flush信号寄存器
  val flush_r = RegNext(io.correctedPC.valid)
  val flush = flush_r || io.correctedPC.valid
  io.isFlush := VecInit(Seq.fill(4)(io.correctedPC.valid))

  // 握手信号
  val handshake = Wire(Vec(3, Bool()))
  val finish = handshake(2)
  val resetHandshake = WireInit(false.B)
  resetHandshake := finish
  handshake(0) := Handshake.master(io.imem.req, true.B, resetHandshake)
  handshake(1) := Handshake.slave(io.imem.resp, handshake(0), resetHandshake)
  handshake(2) := Handshake.master(io.out, handshake(1), resetHandshake)
  val pcUpdate = finish || io.correctedPC.valid

  // 更新flush信号
  when(flush){
    flush_r := ~(!handshake(0) || (handshake(0) && handshake(1)))
    resetHandshake := !handshake(0) || (handshake(0) && handshake(1))
  }

  // 更新PC寄存器
  when(pcUpdate) {
    pcReg := npc
  }

  // 数据路径
  io.imem.req.bits.send(
    addr = pcReg,
    cmd = SimpleBusCmd.read,
    size = 2.U,
    wdata = 0.U,
    wmask = 0.U
  )
  io.out.bits.send(
    pc = pcReg,
    inst = io.imem.resp.bits.rdata
  )
}

