package zen.frontend

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
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
  ysyxsoc: Boolean,
  usePerformanceProbe: Boolean
) extends SerializableModuleParameter

/** Interface of [[IFU]]. */
class IFUInterface(parameter: IFUParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val out = Decoupled(new IFUOutIO(parameter.width))
  val imem = new SimpleBus()
  val isFlush = Input(Bool())
  val correctedPC = Input(UInt(parameter.width.W))
}

/** Hardware Implementation of [[IFU]] */
//yosys:
@instantiable
class IFU(val parameter: IFUParameter)
    extends FixedIORawModule(new IFUInterface(parameter))
    with SerializableModule[IFUParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = s"IFU"
  
  val pcReg = RegInit(if(parameter.ysyxsoc){ "h3000_0000".U(32.W) }else { "h0000_0000".U(32.W) })
  val snpc = pcReg + 4.U // for future, use branch predictor
  val npc = Mux(io.isFlush, io.correctedPC, snpc)

  val flush_r = RegNext(io.isFlush)
  val flush = flush_r || io.isFlush

  val handshake = Wire(Vec(3, Bool()))
  val resetHandshake = Wire(Bool())
  resetHandshake := handshake.reduceTree(_ && _)
  handshake(0) := Handshake.master(io.imem.req, true.B, resetHandshake)
  handshake(1) := Handshake.slave(io.imem.resp, handshake(0), resetHandshake)
  handshake(2) := Handshake.master(io.out, handshake(1), resetHandshake)
  val pcUpdate = handshake.reduceTree(_ && _) || io.isFlush

  when(flush){
    flush_r := ~(!handshake(0) || (handshake(0) && handshake(1)))
    resetHandshake := !handshake(0) || (handshake(0) && handshake(1))
  }

//  // 状态机
//  val start, end, ready_go = Wire(Bool())
//  val s_WaitStart :: s_WaitEnd :: s_WaitFlushEnd :: Nil = Enum(3)
//  val stateF = RegInit(s_WaitStart)
//  val nextStateF = WireDefault(stateF)
//  nextStateF := MuxLookup(stateF, s_WaitStart)(Seq(
//    s_WaitStart   -> Mux(start, Mux(end, s_WaitStart, s_WaitEnd), s_WaitStart),
//    s_WaitEnd     -> Mux(end, s_WaitStart, Mux(io.isFlush, s_WaitFlushEnd, s_WaitEnd)),
//    s_WaitFlushEnd-> Mux(io.imem.resp.valid, s_WaitStart, s_WaitFlushEnd)
//  ))
//  stateF := nextStateF
//
//  // 握手信号
//  io.imem.req.valid := start && stateF === s_WaitStart
//  io.imem.resp.ready := end || (stateF === s_WaitFlushEnd)
//  ready_go := io.imem.resp.valid && (stateF =/= s_WaitFlushEnd)
//  start := io.imem.req.ready && !io.isFlush
//  end := ready_go && io.out.ready
//  io.out.valid := ready_go
//
//  // PC 更新
//  val pcUpdate = ready_go && io.out.ready || io.isFlush
  when(pcUpdate) {
    pcReg := npc
  }

  // 数据路径
  io.imem.req.bits.apply(
    addr = pcReg,
    cmd = SimpleBusCmd.read,
    size = 2.U,
    wdata = 0.U,
    wmask = 0.U
  )
  io.out.bits.apply(
    pc = pcReg,
    inst = io.imem.resp.bits.rdata
  )
}

