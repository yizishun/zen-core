import mainargs._
import zen.backend.fu._
import tb._
import elaborate.Elaborate_ALU._
import chisel3.experimental.util.SerializableModuleElaborator

object ALUFormalMain extends SerializableModuleElaborator {
  val topName = "ALUFormal"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class ALUFormalParameterMain(
    @arg(name = "ALUParameter") ALUParameter: ALUParameterMain,
    @arg(name = "isSBY") isSBY: Boolean,
    @arg(name = "isVCF") isVCF: Boolean) {
    def convert: ALUFormalParameter = ALUFormalParameter(ALUParameter.convert, isSBY, isVCF)
  }

  implicit def ALUParameterMainParser: ParserForClass[ALUParameterMain] =
    ParserForClass[ALUParameterMain]

  implicit def ALUFormalParameterMainParser: ParserForClass[ALUFormalParameterMain] =
    ParserForClass[ALUFormalParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  ALUFormalParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[ALUFormal, ALUFormalParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}
