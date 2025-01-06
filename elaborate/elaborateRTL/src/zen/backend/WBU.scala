package elaborate
import mainargs._
import zen.backend._
import chisel3.experimental.util.SerializableModuleElaborator

object Elaborate_WBU extends SerializableModuleElaborator {
  val topName = "WBU"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class WBUParameterMain(
    @arg(name = "width") width: Int,
    @arg(name = "useAsyncReset") useAsyncReset: Boolean) {
    require(width > 0, "width must be a non-negative integer")
    require(chisel3.util.isPow2(width), "width must be a power of 2")
    def convert: WBUParameter = WBUParameter(width, useAsyncReset)
  }

  implicit def WBUParameterMainParser: ParserForClass[WBUParameterMain] =
    ParserForClass[WBUParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  WBUParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[WBU, WBUParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}