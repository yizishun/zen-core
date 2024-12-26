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

  def simpleBusSlave(io: DecoupledIO[SimpleBusRespBundle], ready: Bool, reset: Bool, burst: Bool = false.B): Bool = {
    val handshake = RegInit(false.B)
    val fire = Wire(Bool())
    when(burst) {
      fire := io.fire && io.bits.isReadLast
    } otherwise {
      fire := io.fire
    }
    io.ready := ready && !handshake
    handshake := handshake | fire
    when(reset) { handshake := false.B }
    handshake || fire
  }

  def simpleBusMaster(io: DecoupledIO[SimpleBusRespBundle], valid: Bool, reset: Bool, burst: Boolean = false, burstLen: Int = 0): Bool = {
    val handshake = RegInit(false.B)
    val (burstCounter, burstDone) = Counter(burst.B && io.fire, burstLen)
    io.valid := valid && !handshake
    io.bits.cmd := Mux(burstDone, SimpleBusCmd.readLast, SimpleBusCmd.read)
    handshake := handshake | (if(burst) burstDone && io.fire else io.fire)
    when(reset) { handshake := false.B }
    handshake || (if(burst) burstDone && io.fire else io.fire)
  }
}
