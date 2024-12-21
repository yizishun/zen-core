package elaborate
import mainargs._
import chisel3.experimental.util.SerializableModuleElaborator
import bus.simplebus._

object Elaborate_SimpleBus2AXI4Converter extends SerializableModuleElaborator {
  val topName = "SimpleBus2AXI4Converter"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  case class SimpleBus2AXI4ConverterParameterMain(
    @arg(name = "useAsyncReset") useAsyncReset: Boolean,
    @arg(name = "lineBeats") lineBeats: Int) {
    require(lineBeats > 0, "lineBeats must be a positive integer")
    def convert: SimpleBus2AXI4ConverterParameter = SimpleBus2AXI4ConverterParameter(useAsyncReset, lineBeats)
  }

  implicit def SimpleBus2AXI4ConverterParameterMainParser: ParserForClass[SimpleBus2AXI4ConverterParameterMain] =
    ParserForClass[SimpleBus2AXI4ConverterParameterMain]

  @main
  def config(
    @arg(name = "parameter") parameter:  SimpleBus2AXI4ConverterParameterMain,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) =
    os.write.over(targetDir / s"${topName}.json", configImpl(parameter.convert))

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[SimpleBus2AXI4Converter, SimpleBus2AXI4ConverterParameter](os.read.stream(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
}

