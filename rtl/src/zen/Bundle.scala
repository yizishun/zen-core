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
  val inst = Output(UInt(xlen.W))
  val pc = Output(UInt(xlen.W))
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
  val brTpe = Output(UInt(2.W))
  val src = Output(Vec(2, UInt(xlen.W)))
  val src3Addr = Output(UInt(xlen.W))

  def issue(fu: UInt, func: UInt, src1: UInt, src2: UInt, src3Addr: UInt, rfWen: Bool, rfDest: UInt, brTpe: UInt): IssueIO = {
    this.fu := fu
    this.func := func
    this.src(0) := src1
    this.src(1) := src2
    this.src3Addr := src3Addr
    this.readyWriteBack.rfWen := rfWen
    this.readyWriteBack.rfDest := rfDest
    this.brTpe := brTpe
    this
  }
}

class ForwardIO(xlen: Int) extends Bundle{
  val wb = new WriteBackIO(xlen)
  val fu = Output(UInt(2.W))
  val valid = Output(Bool())
}

object Src1Type{
  val rs1 = 0.U(1.W)
  val pc = 1.U(1.W)
  def apply() = 1.W
  def isRs1(srcType: UInt): Bool = !srcType
  def isPc(srcType: UInt): Bool = srcType.asBool
}

object Src2Type{
  val rs2 = 0.U(2.W)
  val imm = 1.U(2.W)
  val csrIdx = 2.U(2.W)
  def apply() = 2.W
  def isRs2(tpe: UInt): Bool = !tpe.orR
  def isImm(tpe: UInt): Bool = tpe(0)
  def isCsrIdx(tpe: UInt): Bool = tpe(1)
}

class ControlSignal extends Bundle{
  val fu = Output(UInt(2.W))
  val func = Output(UInt(4.W))
  val brTpe = Output(UInt(2.W))
  val src1Tpe = Output(UInt(Src1Type()))
  val src2Tpe = Output(UInt(Src2Type()))
  val rfSrc1 = Output(UInt(5.W)) //TODO: smaller(16 reg)
  val rfSrc2 = Output(UInt(5.W))
  val rfDest = Output(UInt(5.W))
  val rfWen = Output(Bool())

  def send(fu: UInt, func: UInt, brTpe: UInt, src1Tpe: UInt, src2Tpe: UInt, rfSrc1: UInt, rfSrc2: UInt, rfDest: UInt, rfWen: Bool): ControlSignal = {
    this.fu := fu
    this.func := func
    this.brTpe := brTpe
    this.src1Tpe := src1Tpe
    this.src2Tpe := src2Tpe
    this.rfSrc1 := rfSrc1
    this.rfSrc2 := rfSrc2
    this.rfDest := rfDest
    this.rfWen := rfWen
    this
  }
}

class DecodeIO(xlen: Int) extends Bundle{
  val control = new ControlSignal
  val pc = Output(UInt(xlen.W))
  val imm = Output(UInt(xlen.W))
  val csrIdx = Output(UInt(12.W))
}

class TargetPC(xlen: Int) extends Bundle{
  val pc = Output(UInt(xlen.W))
}
