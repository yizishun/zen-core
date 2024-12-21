package bus.axi4

import chisel3._
import chisel3.util._
import utils.GTimer
import bus.AXI4Parameters

abstract class AXI4BundleBase extends Bundle

/**
  * Common signals of AW and AR channels of AXI4 protocol
  */
abstract class AXI4BundleA extends AXI4BundleBase
{
  val id     = UInt(AXI4Parameters.idBits.W)
  val addr   = UInt(AXI4Parameters.addrBits.W)
  val len    = UInt(AXI4Parameters.lenBits.W)  // number of beats - 1
  val size   = UInt(AXI4Parameters.sizeBits.W) // bytes in beat = 2^size
  val burst  = UInt(AXI4Parameters.burstBits.W)

  // Number of bytes-1 in this operation
  def bytes1(x:Int=0) = {
    val maxShift = 1 << AXI4Parameters.sizeBits
    val tail = ((BigInt(1) << maxShift) - 1).U
    (Cat(len, tail) << size) >> maxShift
  }
}

/**
  * AW channel of AXI4 protocol
  */
class AXI4BundleAW extends AXI4BundleA
{
  override def toPrintable: Printable = {
    p"AW(addr=0x${Hexadecimal(addr)}, len=$len, size=$size, burst=$burst)"
  }
}

/**
  * AR channel of AXI4 protocol
  */
class AXI4BundleAR extends AXI4BundleA
{
  override def toPrintable: Printable = {
    p"AR(addr=0x${Hexadecimal(addr)}, len=$len, size=$size, burst=$burst)"
  }
}

/**
  * W channel of AXI4 protocol
  */
class AXI4BundleW extends AXI4BundleBase
{
  // id ... removed in AXI4
  val data = UInt(AXI4Parameters.dataBits.W)
  val strb = UInt((AXI4Parameters.dataBits/8).W)
  val last = Bool()

  override def toPrintable: Printable = {
    p"W(data=0x${Hexadecimal(data)}, strb=0x${Hexadecimal(strb)}, last=$last)"
  }
}

/**
  * R channel of AXI4 protocol
  */
class AXI4BundleR extends AXI4BundleBase
{
  val id   = UInt(AXI4Parameters.idBits.W)
  val data = UInt(AXI4Parameters.dataBits.W)
  val resp = UInt(AXI4Parameters.respBits.W)
  val last = Bool()

  override def toPrintable: Printable = {
    p"R(id=$id, data=0x${Hexadecimal(data)}, resp=$resp, last=$last)"
  }
}

/**
  * B channel of AXI4 protocol
  */
class AXI4BundleB extends AXI4BundleBase
{
  val id   = UInt(AXI4Parameters.idBits.W)
  val resp = UInt(AXI4Parameters.respBits.W)

  override def toPrintable: Printable = {
    p"B(id=$id, resp=$resp)"
  }
}

/**
  * AXI4 protocol bundle
  */
class AXI4 extends AXI4BundleBase
{
  val aw = Irrevocable(new AXI4BundleAW)
  val w  = Irrevocable(new AXI4BundleW)
  val b  = Flipped(Irrevocable(new AXI4BundleB))
  val ar = Irrevocable(new AXI4BundleAR)
  val r  = Flipped(Irrevocable(new AXI4BundleR))

  def dump(name: String) = {
    when (aw.fire) { printf(p"${GTimer()},[${name}.aw] ${aw.bits}\n") }
    when (w.fire) { printf(p"${GTimer()},[${name}.w] ${w.bits}\n") }
    when (b.fire) { printf(p"${GTimer()},[${name}.b] ${b.bits}\n") }
    when (ar.fire) { printf(p"${GTimer()},[${name}.ar] ${ar.bits}\n") }
    when (r.fire) { printf(p"${GTimer()},[${name}.r] ${r.bits}\n") }
  }
}

object AXI4
{
  def apply() = new AXI4
}
