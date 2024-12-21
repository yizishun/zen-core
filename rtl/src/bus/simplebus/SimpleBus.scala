/**************************************************************************************
* Copyright (c) 2020 Institute of Computing Technology, CAS
* Copyright (c) 2020 University of Chinese Academy of Sciences
*
* NutShell is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*             http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR
* FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

package bus.simplebus

import chisel3._
import chisel3.util._

import utils._
import bus.axi4._
import bus.SimpleBusParameters

sealed abstract class SimpleBusBundle extends Bundle

object SimpleBusCmd {
  // req
                                 //   hit    |    miss
  def read           = "b000".U //  read    |   refill
  def write          = "b001".U //  write   |   refill
  def readBurst      = "b010".U //  read    |   refill
  def writeBurst     = "b011".U //  write   |   refill
  def writeLast      = "b111".U //  write   |   refill

  // resp
  def readLast       = "b110".U
  def writeResp      = "b101".U

  def apply() = UInt(3.W)
}

class SimpleBusReqBundle(
  val dataBits: Int = SimpleBusParameters.dataBits,
  val addrBits: Int = SimpleBusParameters.addrBits,
  val idBits: Int = SimpleBusParameters.idBits,
  ) extends SimpleBusBundle 
  {
  val addr = Output(UInt(addrBits.W))
  val size = Output(UInt(3.W))
  val cmd = Output(SimpleBusCmd())
  val wmask = Output(UInt((dataBits / 8).W))
  val wdata = Output(UInt(dataBits.W))
  val id = if (idBits > 0) Some(Output(UInt(idBits.W))) else None

  override def toPrintable: Printable = {
    p"addr = 0x${Hexadecimal(addr)}, cmd = ${cmd}, size = ${size}, " +
    p"wmask = 0x${Hexadecimal(wmask)}, wdata = 0x${Hexadecimal(wdata)}"
  }

  def apply(addr: UInt, cmd: UInt, size: UInt, wdata: UInt, wmask: UInt, user: UInt = 0.U, id: UInt = 0.U) = {
    this.addr := addr
    this.cmd := cmd
    this.size := size
    this.wdata := wdata
    this.wmask := wmask
    this.id.map(_ := id)
  }

  def isRead() = !cmd(0)
  def isWrite() = cmd(0)
  def isBurst() = cmd(1)
  def isReadBurst() = isRead() && isBurst()
  def isWriteSingle() = isWrite() && !isBurst()
  def isWriteLast() = cmd === SimpleBusCmd.writeLast
}

class SimpleBusRespBundle(
  val idBits: Int = 0,
  val dataBits: Int = SimpleBusParameters.dataBits
  ) extends SimpleBusBundle
  {
  val cmd = Output(SimpleBusCmd())
  val rdata = Output(UInt(dataBits.W))
  val id = if (idBits > 0) Some(Output(UInt(idBits.W))) else None

  override def toPrintable: Printable = p"rdata = ${Hexadecimal(rdata)}, cmd = ${cmd}"

  def isReadLast = cmd === SimpleBusCmd.readLast
  def isWriteResp = cmd === SimpleBusCmd.writeResp
}

// Uncache
class SimpleBus(
  val dataBits: Int = SimpleBusParameters.dataBits,
  val addrBits: Int = SimpleBusParameters.addrBits,
  val idBits: Int = SimpleBusParameters.idBits) 
  extends SimpleBusBundle 
  {
  val req = Decoupled(new SimpleBusReqBundle(dataBits, addrBits, idBits))
  val resp = Flipped(Decoupled(new SimpleBusRespBundle(idBits, dataBits)))

  def isWrite = req.valid && req.bits.isWrite()
  def isRead  = req.valid && req.bits.isRead()
  def toAXI4 = SimpleBus2AXI4Converter(this)

  def dump(name: String) = {
    when (req.fire) { printf(p"${GTimer()},[${name}] ${req.bits}\n") }
    when (resp.fire) { printf(p"${GTimer()},[${name}] ${resp.bits}\n") }
  }
}

class SimpleBusUCExpender() extends Module {
  val io = IO(new Bundle{
    val in = Flipped(new SimpleBus())
    val out = new SimpleBus()
  })
  io.out.req.valid := io.in.req.valid
  io.in.req.ready := io.out.req.ready
  io.out.req.bits.addr := io.in.req.bits.addr
  io.out.req.bits.size := io.in.req.bits.size
  io.out.req.bits.cmd := io.in.req.bits.cmd
  io.out.req.bits.wmask := io.in.req.bits.wmask
  io.out.req.bits.wdata := io.in.req.bits.wdata
  io.in.resp.valid := io.out.resp.valid
  io.out.resp.ready := io.in.resp.ready
  io.in.resp.bits.cmd := io.out.resp.bits.cmd
  io.in.resp.bits.rdata := io.out.resp.bits.rdata
  // DontCare := io.out.resp.bits.user.get
}



