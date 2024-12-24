package utils

import chisel3._
import chisel3.util._
import chisel3.experimental.SerializableModuleParameter

object AddressRegion {
  implicit val rw: upickle.default.ReadWriter[AddressRegion] =
    upickle.default.macroRW
}

case class AddressRegion(
  name: String,
  start: String, // hex string
  size: String,  // hex string
  regionType: String
) extends SerializableModuleParameter {
  def startAddr = BigInt(start.stripPrefix("0x"), 16)
  def sizeVal = BigInt(size.stripPrefix("0x"), 16)
  def endAddr = startAddr + sizeVal - 1

  require(isPow2(sizeVal), s"Size of region $name must be power of 2")
}

object AddressSpaceConfig {
  implicit val rw: upickle.default.ReadWriter[AddressSpaceConfig] =
    upickle.default.macroRW
}

case class AddressSpaceConfig(
  regions: Seq[AddressRegion]
) extends SerializableModuleParameter {
  regions.foreach { region =>
    // 检查地址区域是否重叠
    regions.filter(_ != region).foreach { other =>
      require(
        region.endAddr < other.startAddr || region.startAddr > other.endAddr,
        s"Address region ${region.name} overlaps with ${other.name}"
      )
    }
  }
}

class AddressSpace(config: AddressSpaceConfig) {
  def isInRegion(addr: UInt, name: String): Bool = {
    config.regions.filter(_.name == name).map { region =>
      val bits = log2Ceil(region.sizeVal.toInt)
      (addr ^ region.startAddr.U)(addr.getWidth-1, bits) === 0.U
    }.foldLeft(false.B)(_ || _)
  }

}

object AddressSpace {
  def apply(config: AddressSpaceConfig) = new AddressSpace(config)
}
