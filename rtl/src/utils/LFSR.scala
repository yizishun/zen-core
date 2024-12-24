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

package utils

import chisel3._
import chisel3.util._

object LFSR { 
  // Maximum length LFSR taps for different widths
  private val taps = Map(
    1  -> List(1),  // Special case: toggle flip-flop
    2  -> List(1),
    3  -> List(2),
    4  -> List(3),
    5  -> List(3),
    6  -> List(5),
    7  -> List(6),
    8  -> List(6, 5, 4),
    9  -> List(5),
    10 -> List(7),
    11 -> List(9),
    12 -> List(6, 4, 1),
    13 -> List(4, 3, 1),
    14 -> List(5, 3, 1),
    15 -> List(14),
    16 -> List(14, 13, 11),
    17 -> List(14),
    18 -> List(11),
    19 -> List(6, 2, 1),
    20 -> List(17),
    21 -> List(19),
    22 -> List(21),
    23 -> List(18),
    24 -> List(23, 22, 17),
    25 -> List(22),
    26 -> List(6, 2, 1),
    27 -> List(5, 2, 1),
    28 -> List(25),
    29 -> List(27),
    30 -> List(6, 4, 1),
    31 -> List(28),
    32 -> List(22, 2, 1)
  )

  def apply(width: Int, increment: Bool = true.B): UInt = { 
    require(width >= 1 && width <= 32, "LFSR width must be between 1 and 32 bits")
    require(taps.contains(width), s"Width $width is not supported")

    val lfsr = RegInit(1.U(width.W))
    
    // Special case for width = 1: implement as toggle flip-flop
    if (width == 1) {
      when (increment) {
        lfsr := ~lfsr
      }
    } else {
      // Calculate XOR of tapped bits
      val selectedTaps = taps(width)
      val xorResult = selectedTaps.map(tap => lfsr(tap-1)).reduce(_ ^ _)
      
      when (increment) {
        lfsr := Mux(lfsr === 0.U, 1.U, Cat(xorResult, lfsr(width-1,1)))
      }
    }
    lfsr
  }
}
