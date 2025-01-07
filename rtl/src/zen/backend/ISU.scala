package zen.backend

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util._
import zen._
import utils._

object ISUParameter {
  implicit def rwP: upickle.default.ReadWriter[ISUParameter] =
    upickle.default.macroRW
}

/** Parameter of [[ISU]] */
case class ISUParameter(
  width: Int, 
  useAsyncReset: Boolean,
  rf: RFParameter) 
extends SerializableModuleParameter

/** Interface of [[ISU]]. */
class ISUInterface(parameter: ISUParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val in = Flipped(DecoupledIO(new DecodeIO(parameter.width)))
  val out = DecoupledIO(new IssueIO(parameter.width))
  val wb = Flipped(new WriteBackIO(parameter.width))
  val fwd = Flipped(new ForwardIO(parameter.width))
  val flush = Input(Bool())
}

/** Hardware Implementation of [[ISU]] */
@instantiable
class ISU(val parameter: ISUParameter)
    extends FixedIORawModule(new ISUInterface(parameter))
    with SerializableModule[ISUParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  //handshake
  val handshake = Wire(Vec(2, Bool()))
  val resetHandshake = handshake(1)
  val canIssue = Wire(Bool())
  handshake(0) := Handshake.slave(io.in, true.B, resetHandshake)
  handshake(1) := Handshake.master(io.out, handshake(0) && canIssue, resetHandshake)

  def isDepend(rfSrc: UInt, rfDest: UInt, wen: Bool): Bool = (rfSrc =/= 0.U) && (rfSrc === rfDest) && wen
  
  val (rfSrc1, rfSrc2, rfWen, rfDest) = (io.in.bits.control.rfSrc1, io.in.bits.control.rfSrc2, io.in.bits.control.rfWen, io.in.bits.control.rfDest)
  val rf = new RF(parameter.rf)
  //准备好src1和src2
  //四个条件
  //1.可以从exu转发得到
  //2.可以从wb转发得到
  //3.sb空闲
  //4.src1和src2的类型不是reg
  val src1CanForwardFromExu = isDepend(rfSrc1, io.fwd.wb.rfDest, io.fwd.wb.rfWen && io.fwd.valid)
  val src2CanForwardFromExu = isDepend(rfSrc2, io.fwd.wb.rfDest, io.fwd.wb.rfWen && io.fwd.valid)
  val src1CanForwardFromWb  = isDepend(rfSrc1, io.wb.rfDest, io.wb.rfWen) && !src1CanForwardFromExu
  val src2CanForwardFromWb  = isDepend(rfSrc2, io.wb.rfDest, io.wb.rfWen) && !src2CanForwardFromExu
  val sb = new ScoreBoard(parameter.rf)

  val src1Ready = src1CanForwardFromExu || src1CanForwardFromWb || !sb.isBusy(rfSrc1) || !SrcType.isReg(io.in.bits.control.src1Tpe)
  val src2Ready = src2CanForwardFromExu || src2CanForwardFromWb || !sb.isBusy(rfSrc2) || !SrcType.isReg(io.in.bits.control.src2Tpe)
  val wdataReady = src2CanForwardFromExu || src2CanForwardFromWb || !sb.isBusy(rfSrc2)

  canIssue := src1Ready && src2Ready && wdataReady

  //获取src1和src2真正的值
  //同样四个来源
  val src1Value = Mux1H(Seq(
    src1CanForwardFromExu -> io.fwd.wb.rfWdata,
    src1CanForwardFromWb -> io.wb.rfWdata,
    !sb.isBusy(rfSrc1) -> rf.read(rfSrc1),
    !SrcType.isReg(io.in.bits.control.src1Tpe) -> io.in.bits.pc
  ))
  val src2Value = Mux1H(Seq(
    src2CanForwardFromExu -> io.fwd.wb.rfWdata,
    src2CanForwardFromWb -> io.wb.rfWdata,
    !sb.isBusy(rfSrc2) -> rf.read(rfSrc2),
    !SrcType.isReg(io.in.bits.control.src2Tpe) -> io.in.bits.imm
  ))
  val wdataValue = Mux1H(Seq(
    src2CanForwardFromExu -> io.fwd.wb.rfWdata,
    src2CanForwardFromWb -> io.wb.rfWdata,
    !sb.isBusy(rfSrc2) -> rf.read(rfSrc2),
  ))

  //issue
  io.out.bits.issue(
    fu = io.in.bits.control.fu,
    func = io.in.bits.control.func,
    src1 = src1Value,
    src2 = src2Value,
    wdata = wdataValue,
    rfWen = io.in.bits.control.rfWen,
    rfDest = io.in.bits.control.rfDest
  )

  when(io.wb.rfWen){ rf.write(io.wb.rfDest, io.wb.rfWdata) }
  //更新scoreboard
  val wbClearMask = Mux(io.wb.rfWen && !isDepend(io.wb.rfDest, io.fwd.wb.rfDest, io.fwd.wb.rfWen), sb.mask(io.wb.rfDest), 0.U(parameter.rf.regNum.W))
  val isuFireSetMask = Mux(io.out.fire, sb.mask(rfDest), 0.U)
  when (io.flush) { sb.update(0.U, Fill(parameter.rf.regNum, 1.U(1.W))) }
  .otherwise { sb.update(isuFireSetMask, wbClearMask) }
}
