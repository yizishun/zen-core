package elaborate
import mainargs._
import rtl._

object Elaborate_jk extends SerializableModuleElaborator {
  val topName = "JK"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class JKParameterMain(
    @arg(name = "width") width: Int,
    @arg(name = "useAsyncReset") useAsyncReset: Boolean) {
    require(width > 0, "width must be a non-negative integer")
    require(chisel3.util.isPow2(width), "width must be a power of 2")
    def convert: JKParameter = JKParameter(width, useAsyncReset)
  }

  implicit def JKParameterMainParser: ParserForClass[JKParameterMain] =
    ParserForClass[JKParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  JKParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[JK, JKParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}