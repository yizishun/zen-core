package elaborate
import mainargs._
import zen.backend.fu._
import chisel3.experimental.util.SerializableModuleElaborator

object Elaborate_CSR extends SerializableModuleElaborator {
  val topName = "CSR"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class CSRParameterMain(
    @arg(name = "width") width: Int,
    @arg(name = "useAsyncReset") useAsyncReset: Boolean) {
    require(width > 0, "width must be a non-negative integer")
    require(chisel3.util.isPow2(width), "width must be a power of 2")
    def convert: CSRParameter = CSRParameter(width, useAsyncReset)
  }

  implicit def CSRParameterMainParser: ParserForClass[CSRParameterMain] =
    ParserForClass[CSRParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  CSRParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[CSR, CSRParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}