import mainargs._
import chisel3.experimental.util.SerializableModuleElaborator
import tb._
import chisel3.experimental.SerializableModuleParameter

object ZenTestBenchMain extends SerializableModuleElaborator {
  val topName = "ZenTestBench"

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  @main
  def config(
    @arg(name = "parameter") parameter: os.Path
  ): Unit = ()

  @main
  def design(
    @arg(name = "parameter") parameter:  os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[ZenTestBench, ZenTestBenchParameter](os.read(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
} 