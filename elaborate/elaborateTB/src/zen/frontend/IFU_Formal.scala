import mainargs._
import zen.frontend._
import tb._
import elaborate.Elaborate_IFU._
import chisel3.experimental.util.SerializableModuleElaborator

object IFUFormalMain extends SerializableModuleElaborator {
  val topName = "IFUFormal"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class IFUFormalParameterMain(
    @arg(name = "IFUParameter") IFUParameter: IFUParameterMain) {
    def convert: IFUFormalParameter = IFUFormalParameter(IFUParameter.convert)
  }

  implicit def IFUParameterMainParser: ParserForClass[IFUParameterMain] =
    ParserForClass[IFUParameterMain]

  implicit def IFUFormalParameterMainParser: ParserForClass[IFUFormalParameterMain] =
    ParserForClass[IFUFormalParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  IFUFormalParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[IFUFormal, IFUFormalParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}

