package utils

import chisel3._
import chisel3.util._

object PipelineConnect {
  def apply[T <: Data, T2 <: Data](prevOut: DecoupledIO[T], thisIn: DecoupledIO[T]) = {
    prevOut.ready := thisIn.ready
    thisIn.bits := RegEnable(prevOut.bits, prevOut.valid && thisIn.ready)
    thisIn.valid := RegEnable(prevOut.valid, thisIn.ready);
  }
}

