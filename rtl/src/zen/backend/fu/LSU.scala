package zen.backend.fu

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate, Definition}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.util._
import zen._
import bus.simplebus._
import utils._

object LSUParameter {
  implicit def rwP: upickle.default.ReadWriter[LSUParameter] =
    upickle.default.macroRW
}

/** Parameter of LSU */
case class LSUParameter(
  width: Int,
  useAsyncReset: Boolean
) extends SerializableModuleParameter

object LSUOp{
  def LSU_LB =  "b0000".U(4.W)
  def LSU_LH =  "b0001".U(4.W)
  def LSU_LW =  "b0010".U(4.W)
  def LSU_LBU = "b0100".U(4.W)
  def LSU_LHU = "b0101".U(4.W)
  def LSU_SB =  "b1000".U(4.W)
  def LSU_SH =  "b1001".U(4.W)
  def LSU_SW =  "b1010".U(4.W)

  def isLoad(func: UInt): Bool = func(3)
  def isStore(func: UInt): Bool = !func(3)
}

/** Interface of LSU */
class LSUInterface(parameter: LSUParameter) extends FunctionIO(parameter.width, LSUOp.LSU_LB.getWidth) {
  val clock = Input(Clock())
  val reset = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val dmem = new SimpleBus()
}

//TODO: Flush
@instantiable
class LSU(val parameter: LSUParameter) 
    extends FixedIORawModule(new LSUInterface(parameter)) 
    with SerializableModule[LSUParameter] 
    with ImplicitClock 
    with ImplicitReset {

  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = s"LSU"

  val (func, addr, wdata) = (io.in.bits.func, io.in.bits.src(0), io.in.bits.src(1))

  val handshake = Wire(Vec(4, Bool()))
  val finish = handshake(3)
  val resetHandshake = WireInit(false.B)
  resetHandshake := finish
  handshake(0) := Handshake.slave(io.in, true.B, resetHandshake)
  handshake(1) := Handshake.master(io.dmem.req, handshake(0), resetHandshake)
  handshake(2) := Handshake.slave(io.dmem.resp, handshake(1), resetHandshake)
  handshake(3) := Handshake.master(io.out, handshake(2), resetHandshake)

  //发出dmem请求
  val sizeEncode = io.in.bits.func(1, 0)
  io.dmem.req.bits.send(
    addr = addr,
    cmd = Mux(LSUOp.isLoad(func), SimpleBusCmd.read, SimpleBusCmd.write),
    size = sizeEncode,
    wdata = genWdata(wdata, sizeEncode),
    wmask = genWmask(addr, sizeEncode),
  )
  def genWmask(addr: UInt, sizeEncode: UInt): UInt = {
    (MuxLookup(sizeEncode, 0x1.U(4.W))(Seq(
      "b00".U -> 0x1.U(4.W),  //0001 << addr(2:0) //TODO: parameterize it with width
      "b01".U -> 0x3.U(4.W),  //0011
      "b10".U -> 0xf.U(4.W),  //1111
    )) << addr(1, 0))
  }

  def genWdata(data: UInt, sizeEncode: UInt): UInt = {
    MuxLookup(sizeEncode, data)(Seq(
      "b00".U -> Fill(4, data(7, 0)), //TODO: parameterize it with width
      "b01".U -> Fill(2, data(15, 0)),
      "b10".U -> data,
    ))
  }

  //接受dmem的回复
  val (rdata, rcmd) = io.dmem.resp.bits.get()
  io.out.bits := genRdata(rdata, func(2, 0))

  def genRdata(rdata: UInt, op: UInt): UInt = {
    val rdataVec = rdata.asTypeOf(Vec(4, UInt(8.W))) //TODO: parameterize it with width
    val result = MuxLookup(op, rdata)(Seq(
      "b000".U -> rdataVec(addr(1, 0)).asSInt.pad(parameter.width).asUInt,
      "b001".U -> Cat(rdataVec(addr(1, 0) + 1.U), rdataVec(addr(1, 0))).asSInt.pad(parameter.width).asUInt,
      "b010".U -> rdata,
      "b100".U -> rdataVec(addr(1, 0)).pad(parameter.width),
      "b101".U -> Cat(rdataVec(addr(1, 0) + 1.U), rdataVec(addr(1, 0))).pad(parameter.width)
    ))
    result
  }
}
/**
 * in
 *  src1: src3Addr
 *  src2: rs2
 *  func: lsuOp
 * out
 *  out: rdata
 */
object LSU {
  def apply(parameter: LSUParameter, clock: Clock, reset: Reset, valid: Bool, src1: UInt, src2: UInt, func: UInt): Instance[LSU] = {
    val lsu = Instantiate(new LSU(parameter))
    lsu.io.clock := clock
    lsu.io.reset := reset
    lsu.io.in.valid := valid
    lsu.io.in.bits.src(0) := src1
    lsu.io.in.bits.src(1) := src2
    lsu.io.in.bits.func := func
    lsu
  }
}
