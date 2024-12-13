package utils

import chisel3._
import chisel3.util._

/**
  * Insert a register between two signals.
  * But it doesn't affect the area if the enable signal is false.
  *
  * @param x      The signal to be registered.
  * @param enable Whether to enable the register.
  * @tparam T The type of the signal.
  * @return The registered signal.
  */
object InsertReg {
  def apply[T <: Data](x: T, enable: Boolean, defaultVal: T = null.asInstanceOf[T]): T = {
    if (enable) {
      if (defaultVal != null) RegNext(x, defaultVal) else RegNext(x)
    } else x
  }

  def apply[T <: Data](xs: Seq[T], enable: Boolean): Seq[T] = {
    xs.map(x => apply(x, enable))
  }

  implicit class InsertRegOps[T <: Data](x: T) {
    def withReg(enable: Boolean, defaultVal: T = null.asInstanceOf[T]): T = {
      InsertReg(x, enable, defaultVal)
    }
  }
}