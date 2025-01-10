package zen.backend

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate, Definition}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.probe.{define, Probe, ProbeValue}
import chisel3.properties.{AnyClassType, Class, Property}
import chisel3.util._
import zen.backend.fu._
import zen._
import bus.simplebus._
import utils._

object EXUParameter {
  implicit def rwP: upickle.default.ReadWriter[EXUParameter] =
    upickle.default.macroRW
}

object FU {
  val CSR = "b00".U(2.W)
  val LSU = "b01".U(2.W)
  val ALU = "b10".U(2.W)

  def isCSR(fu: UInt) = fu === 0.U
  def isLSU(fu: UInt) = fu(0)
  def isALU(fu: UInt) = fu(1)
}

/** Parameter of [[EXU]] */
case class EXUParameter(
  width: Int,
  useAsyncReset: Boolean,
  alu: ALUParameter,
  csr: CSRParameter,
  lsu: LSUParameter
) extends SerializableModuleParameter

/** Interface of [[EXU]]. */
class EXUInterface(parameter: EXUParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val in = Flipped(Decoupled(new IssueIO(parameter.width)))
  val out = Decoupled(new WriteBackIO(parameter.width))
  val dmem = new SimpleBus()
  val isFlush = Input(Bool())
  val forward = new ForwardIO(parameter.width)
  val targetPC = Valid(new TargetPC(parameter.width))
}

//TODO: Flush
/** Hardware Implementation of EXU */
@instantiable
class EXU(val parameter: EXUParameter)
    extends FixedIORawModule(new EXUInterface(parameter))
    with SerializableModule[EXUParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset

  //handshake and setup
  val handshake = Wire(Vec(2, Bool()))
  val resetHandshake = handshake(1)
  val exeDone = Wire(Bool())
  handshake(0) := Handshake.slave(io.in, true.B, resetHandshake)
  handshake(1) := Handshake.master(io.out, handshake(0) && exeDone, resetHandshake)

  //ALU
  val alu = ALU(parameter.alu, io.clock, io.reset, 
    valid = handshake(0) && FU.isALU(io.in.bits.fu), 
    src1 = io.in.bits.src(0), 
    src2 = io.in.bits.src(1), 
    func = io.in.bits.func,
    brTpe = io.in.bits.brTpe)
  alu.io.out.ready := io.out.ready
  //CSR
  val csr = CSR(parameter.csr, io.clock, io.reset, 
    valid = handshake(0) && FU.isCSR(io.in.bits.fu), 
    src1 = io.in.bits.src(0), 
    src2 = io.in.bits.src(1), 
    func = io.in.bits.func)
  csr.io.out.ready := io.out.ready
  //LSU
  val lsu = LSU(parameter.lsu, io.clock, io.reset, 
    valid = handshake(0) && FU.isLSU(io.in.bits.fu), 
    src1 = io.in.bits.src3Addr, 
    src2 = io.in.bits.src(1), 
    func = io.in.bits.func)
  lsu.io.dmem <> io.dmem
  lsu.io.out.ready := io.out.ready

  exeDone := alu.io.out.valid | csr.io.out.valid | lsu.io.out.valid

  //to wbu
  io.out.bits.waiveAs[ReadyWriteBackIO](_.rfWdata) :<>= (io.in.bits.readyWriteBack: ReadyWriteBackIO)
  io.out.bits.rfWdata := Mux(io.in.bits.fu === FU.CSR, csr.io.out.bits, 
                             Mux(io.in.bits.fu === FU.LSU, lsu.io.out.bits, alu.io.out.bits))

  //forward
  io.forward.wb := io.out.bits
  io.forward.fu := io.in.bits.fu
  io.forward.valid := exeDone
  //redirect pc
  io.targetPC.bits.pc := Mux(alu.io.targetPC.valid, io.in.bits.src3Addr, 
                            Mux(csr.io.targetPC.valid, csr.io.targetPC.bits.pc, 0.U)) //多发射的时候肯定要改这里
  io.targetPC.valid := alu.io.targetPC.valid | csr.io.targetPC.valid
}
