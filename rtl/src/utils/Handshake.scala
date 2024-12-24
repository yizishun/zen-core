package utils
import chisel3._
import chisel3.util._
import bus.simplebus._

object Handshake {
  def master[T <: Data](io: DecoupledIO[T], valid: Bool, reset: Bool): Bool = {
    val handshake = RegInit(false.B)
    io.valid := valid && !handshake
    handshake := handshake | io.fire
    when(reset) { handshake := false.B }
    handshake || io.fire
  }

  def slave[T <: Data](io: DecoupledIO[T], ready: Bool, reset: Bool): Bool = {
    val handshake = RegInit(false.B)
    io.ready := ready && !handshake
    handshake := handshake | io.fire
    when(reset) { handshake := false.B }
    handshake || io.fire
  }

  def simpleBusSlave(io: DecoupledIO[SimpleBusRespBundle], ready: Bool, reset: Bool, burst: Boolean = false): Bool = {
    val handshake = RegInit(false.B)
    io.ready := ready && !handshake
    handshake := handshake | (if(burst) io.fire else io.fire && io.bits.isReadLast)
    when(reset) { handshake := false.B }
    handshake || (if(burst) io.fire else io.fire && io.bits.isReadLast)
  }
}