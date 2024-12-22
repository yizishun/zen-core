package elaborate
import mainargs._
import chisel3.experimental.util.SerializableModuleElaborator
import zen.frontend._
import chisel3.internal.naming.DummyNamer.name

object Elaborate_IFU extends SerializableModuleElaborator {
  val topName = "IFU"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class IFUParameterMain(
    @arg(name = "width") width: Int,
    @arg(name = "useAsyncReset") useAsyncReset: Boolean,
    @arg(name = "ysyxsoc") ysyxsoc: Boolean,
    @arg(name = "usePerformanceProbe") usePerformanceProbe: Boolean) {
    require(width > 0, "width must be a non-negative integer")
    require(chisel3.util.isPow2(width), "width must be a power of 2")
    def convert: IFUParameter = IFUParameter(width, useAsyncReset, ysyxsoc, usePerformanceProbe)
  }

  implicit def IFUParameterMainParser: ParserForClass[IFUParameterMain] =
    ParserForClass[IFUParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  IFUParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[IFU, IFUParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}

