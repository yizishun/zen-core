package utils
import chisel3._
import chisel3.util._

object Handshake{
  def master[T <: Data](io: DecoupledIO[T], valid: Bool, reset: Bool): Bool = {
    // 在其中维护一个reg，指示当前是否已经握手过了
    val handshake = RegInit(false.B)
    // 如果握手过了，那么把valid置0
    io.valid := valid && !handshake
    // handshake在fire时被拉高，并且不能被拉低
    handshake := Mux(handshake, true.B, io.fire)
    // reset时，handshake才能被拉低
    when(reset){
      handshake := false.B
    }
    // 返回是否已经握手过了
    handshake || io.fire
  }

  def slave[T <: Data](io: DecoupledIO[T], ready: Bool, reset: Bool): Bool = {
    val handshake = RegInit(false.B)
    io.ready := ready && !handshake
    handshake := Mux(handshake, true.B, io.fire)
    when(reset){
      handshake := false.B
    }
    handshake || io.fire
  }
}