package zen
import chisel3._
import chisel3.util._

class State extends Bundle{
  val state = Bool()
  val stateNum = UInt(8.W) //TODO: smaller

  def apply(state: Bool, stateNum: UInt): State = {
    this.state := state
    this.stateNum := stateNum
    this
  }
}

class IFUOutIO(xlen: Int) extends Bundle{
  val inst = Output(UInt(32.W))
  val pc = Output(UInt(32.W))
  //val instState = Output(new State) //TODO: consider rresp if area is sufficient

  def apply(inst: UInt, pc: UInt) = {
    this.inst := inst
    this.pc := pc
    //this.instState := instState
  }
}
