package elaborate
import mainargs._
import chisel3.experimental.util.SerializableModuleElaborator
import zen.frontend._
import utils._
import chisel3.experimental.SerializableModuleParameter

object Elaborate_ICache extends SerializableModuleElaborator {
  val topName = "ICache"

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
    val (firrtl, annos) = designImpl[ICache, ICacheParameter](os.read(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
} 