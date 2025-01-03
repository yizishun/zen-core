package utils

import chisel3._
import chisel3.util._

object RegMap {
  def apply(addr: Int, reg: UInt) = (addr, reg)
  def generate(mapping: Map[Int, UInt], raddr: UInt, rdata: UInt,
    waddr: UInt, wen: Bool, wdata: UInt):Unit = {
    val chiselMapping = mapping.map { case (a, r) => (a.U, r) }
    rdata := MuxLookup(raddr, 0.U)(chiselMapping.map { case (a, r) => (a -> r) }.toSeq)
    chiselMapping.map { case (a, r) =>
      when (wen && waddr === a) { r := wdata }
    }
  }
}

