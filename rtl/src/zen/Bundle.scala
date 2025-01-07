package zen
import chisel3._
import chisel3.util._

class State extends Bundle{
  val state = Bool()
  val stateNum = UInt(8.W) //TODO: smaller

  def send(state: Bool, stateNum: UInt): State = {
    this.state := state
    this.stateNum := stateNum
    this
  }

  def get = (state, stateNum)
}

class IFUOutIO(xlen: Int) extends Bundle{
  val inst = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
  //val instState = Output(new State) //TODO: consider rresp if area is sufficient

  def send(inst: UInt, pc: UInt) = {
    this.inst := inst
    this.pc := pc
    //this.instState := instState
  }

  def get = (inst, pc)
}

class FunctionIO(xlen: Int, fuWidth: Int) extends Bundle{
  val in = Flipped(Decoupled(new Bundle{
    val func = UInt(fuWidth.W)
    val src = Vec(2, UInt(xlen.W))
  }))
  val out = Decoupled(UInt(xlen.W))
}

class ReadyWriteBackIO(xlen: Int) extends Bundle{
  val rfWen = Output(Bool())
  val rfDest = Output(UInt(5.W))
}

class WriteBackIO(xlen: Int) extends ReadyWriteBackIO(xlen){
  val rfWdata = Output(UInt(xlen.W))
}

class IssueIO(xlen: Int) extends Bundle{
  val readyWriteBack = new ReadyWriteBackIO(xlen)
  val fu = Output(UInt(2.W))
  val func = Output(UInt(4.W))
  val src = Output(Vec(2, UInt(xlen.W)))
  val wdata = Output(UInt(xlen.W))

  def issue(fu: UInt, func: UInt, src1: UInt, src2: UInt, wdata: UInt, rfWen: Bool, rfDest: UInt): IssueIO = {
    this.fu := fu
    this.func := func
    this.src(0) := src1
    this.src(1) := src2
    this.wdata := wdata
    this.readyWriteBack.rfWen := rfWen
    this.readyWriteBack.rfDest := rfDest
    this
  }
}

class ForwardIO(xlen: Int) extends Bundle{
  val wb = new WriteBackIO(xlen)
  val fu = Output(UInt(2.W))
  val valid = Output(Bool())
}

object SrcType{
  val reg = 0.U
  val imm = 1.U
  val pc = 1.U
  def apply() = 1.W
  def isReg(srcType: UInt) = !srcType
  def isImm(srcType: UInt) = srcType
  def isPc(srcType: UInt) = srcType
}

class ControlSignal extends Bundle{
  val fu = Output(UInt(2.W))
  val func = Output(UInt(4.W))
  val src1Tpe = Output(UInt(SrcType()))
  val src2Tpe = Output(UInt(SrcType()))
  val rfSrc1 = Output(UInt(5.W)) //TODO: smaller(16 reg)
  val rfSrc2 = Output(UInt(5.W))
  val rfDest = Output(UInt(5.W))
  val rfWen = Output(Bool())
}

class DecodeIO(xlen: Int) extends Bundle{
  val control = new ControlSignal
  val pc = Output(UInt(xlen.W))
  val imm = Output(UInt(xlen.W))
}
