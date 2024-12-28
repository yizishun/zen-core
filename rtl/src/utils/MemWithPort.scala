package utils

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util._

/** Parameter of [[MemWithPort]] that can be serialized */
case class MemWithPortParameter(
    set: Int,
    way: Int,
    numReadPorts: Int,
    numWritePorts: Int,
    numReadwritePorts: Int,
    useAsyncReset: Boolean
) extends SerializableModuleParameter

object MemWithPortParameter {
  implicit def rwP: upickle.default.ReadWriter[MemWithPortParameter] =
    upickle.default.macroRW
}

/** A bundle of signals representing a memory read port. */
class MemReadPort[T <: Data](tpe: T, addrWidth: Int, way: Int) extends Bundle {
  val address = Input(UInt(addrWidth.W))
  val enable = Input(Bool())
  val data = Output(Vec(way, tpe))
}

/** A bundle of signals representing a memory write port. */
class MemWritePort[T <: Data](tpe: T, addrWidth: Int, way: Int) extends Bundle {
  val address = Input(UInt(addrWidth.W))
  val enable = Input(Bool())
  val waymask = Input(Vec(way, Bool()))
  val data = Input(tpe)
}

/** A bundle of signals representing a memory read/write port. */
class MemReadWritePort[T <: Data](tpe: T, addrWidth: Int, way: Int) extends Bundle {
  val address = Input(UInt(addrWidth.W))
  val enable = Input(Bool())
  val isWrite = Input(Bool())
  val waymask = Input(Vec(way, Bool()))
  val readData = Output(Vec(way, tpe))
  val writeData = Input(tpe)
}

/** Interface of [[MemWithPort]]. */
class MemWithPortInterface[T <: Data](tpe: T)(implicit val parameter: MemWithPortParameter) extends Bundle {
  val clock = Input(Clock())
  val reset = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  
  private val addrWidth = log2Up(parameter.set)
  val readPorts = Vec(parameter.numReadPorts, new MemReadPort(tpe, addrWidth, parameter.way))
  val writePorts = Vec(parameter.numWritePorts, new MemWritePort(tpe, addrWidth, parameter.way))
  val readwritePorts = Vec(parameter.numReadwritePorts, new MemReadWritePort(tpe, addrWidth, parameter.way))

  def read(address: UInt, portIdx: Int = 0): Vec[T] = {
    require(portIdx >= 0 && portIdx < parameter.numReadPorts, s"Read port index $portIdx out of range")
    readPorts(portIdx).address := address
    readPorts(portIdx).enable := true.B
    readPorts(portIdx).data.asTypeOf(Vec(parameter.way, tpe))
  }

  def write(address: UInt, data: T, waymask: Vec[Bool], enable: Bool, portIdx: Int = 0): Unit = {
    require(portIdx >= 0 && portIdx < parameter.numWritePorts, s"Write port index $portIdx out of range")
    writePorts(portIdx).address := address
    writePorts(portIdx).data := data
    writePorts(portIdx).waymask := waymask
    writePorts(portIdx).enable := enable
  }

  def readWrite(
      address: UInt,
      isWrite: Bool,
      writeData: T,
      waymask: Vec[Bool],
      portIdx: Int = 0
  ): Vec[T] = {
    require(portIdx >= 0 && portIdx < parameter.numReadwritePorts, s"Read/write port index $portIdx out of range")
    readwritePorts(portIdx).address := address
    readwritePorts(portIdx).isWrite := isWrite
    readwritePorts(portIdx).writeData := writeData
    readwritePorts(portIdx).waymask := waymask
    readwritePorts(portIdx).enable := true.B
    readwritePorts(portIdx).readData.asTypeOf(Vec(parameter.way, tpe))
  }
}

/** A memory interface with configurable number of read, write and read/write ports.
  * Uses combinational read logic for zero-cycle latency.
  */
@instantiable
class MemWithPort[T <: Data](tpe: T)(implicit val parameter: MemWithPortParameter)
    extends FixedIORawModule(new MemWithPortInterface(tpe))
    with SerializableModule[MemWithPortParameter]
    with ImplicitClock
    with ImplicitReset {

  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = s"MemWithPort_${parameter.way}Way_${parameter.numReadPorts}R${parameter.numWritePorts}W${parameter.numReadwritePorts}RW"

  require(parameter.set > 0, "Memory size must be positive")
  require(parameter.way > 0, "Number of ways must be positive")
  require(parameter.numReadPorts >= 0, "Number of read ports must be non-negative")
  require(parameter.numWritePorts >= 0, "Number of write ports must be non-negative")
  require(parameter.numReadwritePorts >= 0, "Number of read/write ports must be non-negative")
  require(
    parameter.numReadPorts + parameter.numWritePorts + parameter.numReadwritePorts > 0,
    "At least one port must be specified"
  )

  // Create the memory array for each way with UInt type
  val wordType = UInt(tpe.getWidth.W)
  val mem = Mem(parameter.set, Vec(parameter.way, wordType))

  // Connect read ports with combinational read
  io.readPorts.foreach { port =>
    port.data := DontCare
    when(port.enable) {
      port.data := mem.read(port.address).map(_.asTypeOf(tpe))
    }
  }

  // Connect write ports
  io.writePorts.foreach { port =>
    when(port.enable) {
      val mask = port.waymask
      val writeData = VecInit(Seq.fill(parameter.way)(port.data.asUInt))
      mem.write(port.address, writeData, mask, io.clock)
    }
  }

  // Connect read/write ports with combinational read
  io.readwritePorts.foreach { port =>
    port.readData := DontCare
    when(port.enable) {
      val rdwrPort = mem(port.address)
      when(port.isWrite) {
        val writeData = VecInit(Seq.fill(parameter.way)(port.writeData.asUInt))
        port.waymask.zipWithIndex.foreach { case (waymask, idx) =>
          when(waymask) {
            rdwrPort(idx) := writeData(idx)
          }
        }
      }.otherwise {
        port.readData := rdwrPort.map(_.asTypeOf(tpe))
      }
    }
  }
}

object MemWithPort {
  def apply[T <: Data](parameter: MemWithPortParameter, tpe: T): MemWithPortInterface[T] = {
    implicit val p: MemWithPortParameter = parameter
    val inst = Instantiate(new MemWithPort(tpe))
    inst.io
  }

  def apply[T <: Data](
      size: Int,
      tpe: T,
      way: Int = 1,
      numReadPorts: Int = 1,
      numWritePorts: Int = 1,
      numReadwritePorts: Int = 0,
      useAsyncReset: Boolean = false
  ): MemWithPortInterface[T] = {
    apply(MemWithPortParameter(size, way, numReadPorts, numWritePorts, numReadwritePorts, useAsyncReset), tpe)
  }
}
