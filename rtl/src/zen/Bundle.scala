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