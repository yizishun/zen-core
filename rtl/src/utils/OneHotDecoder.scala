package utils

import chisel3._
import chisel3.util._

object OneHotDecoder {
  /** 将二进制值转换为one-hot编码
   * @param in 输入的二进制值
   * @param width one-hot编码的宽度（可选），默认为输入值能表示的最大范围
   * @return one-hot编码的输出，Vec[Bool]类型
   */
  def apply(in: UInt, width: Option[Int] = None): Vec[Bool] = {
    val w = width.getOrElse(1 << in.getWidth)
    require(w >= (1 << in.getWidth), "One-hot width must be greater than or equal to 2^(input width)")
    
    val onehot = (1.U << in)(w-1, 0)
    VecInit(onehot.asBools)
  }

  /** 将二进制值转换为one-hot编码，并进行有效性检查
   * @param in 输入的二进制值
   * @param width one-hot编码的宽度
   * @param valid 输入是否有效的信号
   * @return (one-hot编码的输出Vec[Bool], 有效性检查结果)
   */
  def withValid(in: UInt, width: Int, valid: Bool): (Vec[Bool], Bool) = {
    val isValid = valid && (in < width.U)
    val onehot = Mux(isValid, (1.U << in)(width-1, 0), 0.U(width.W))
    (VecInit(onehot.asBools), isValid)
  }
}