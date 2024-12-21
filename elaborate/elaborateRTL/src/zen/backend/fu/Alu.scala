package elaborate
import mainargs._
import chisel3.experimental.util.SerializableModuleElaborator
import zen.backend.fu._

object Elaborate_ALU extends SerializableModuleElaborator {
  val topName = "ALU"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class ALUParameterMain(
    @arg(name = "width") width: Int,
    @arg(name = "useAsyncReset") useAsyncReset: Boolean,
    @arg(name = "hasOf") hasOf: Boolean,
    @arg(name = "hasZf") hasZf: Boolean,
    @arg(name = "hasNf") hasNf: Boolean,
    @arg(name = "hasCf") hasCf: Boolean,
    @arg(name = "analysisTiming") analysisTiming: Boolean) {
    require(width > 0, "width must be a non-negative integer")
    require(chisel3.util.isPow2(width), "width must be a power of 2")
    def convert: ALUParameter = ALUParameter(width, useAsyncReset, hasOf, hasZf, hasNf, hasCf, analysisTiming)
  }

  implicit def ALUParameterMainParser: ParserForClass[ALUParameterMain] =
    ParserForClass[ALUParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  ALUParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[ALU, ALUParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}