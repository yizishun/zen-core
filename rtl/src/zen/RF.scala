package zen

import chisel3._
import chisel3.util._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
object RFParameter {
  implicit def rwP: upickle.default.ReadWriter[RFParameter] =
    upickle.default.macroRW
}

/** Parameter of [[RF]] */
case class RFParameter(
  width: Int,
  regNum: Int,
) extends SerializableModuleParameter

class RF(parameter: RFParameter) {
  val rf = Mem(parameter.regNum, UInt(parameter.width.W))
  //val rf = RegInit(VecInit(Seq.fill(parameter.regNum)(0.U(parameter.width.W))))

  def read(addr: UInt): UInt = {
    Mux(addr === 0.U, 0.U, rf(addr))
  }

  def write(addr: UInt, data: UInt): Unit = {
    rf(addr) := data(parameter.width - 1, 0)
  }
}

//copy from nutshell
class ScoreBoard(parameter: RFParameter) {
  val busy = RegInit(0.U(parameter.regNum.W))
  def isBusy(idx: UInt): Bool = busy(idx)
  def mask(idx: UInt) = (1.U(parameter.regNum.W) << idx)(parameter.regNum-1, 0)
  def update(setMask: UInt, clearMask: UInt) = {
    // When clearMask(i) and setMask(i) are both set, setMask(i) wins.
    // This can correctly record the busy bit when reg(i) is written
    // and issued at the same cycle.
    // Note that rf(0) is always free.
    busy := Cat(((busy & ~clearMask) | setMask)(parameter.regNum-1, 1), 0.U(1.W))
  }
}
