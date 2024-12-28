package elaborate

import mainargs._
import chisel3.experimental.util.SerializableModuleElaborator
import device._
import chisel3.experimental.SerializableModuleParameter

object Elaborate_Clint extends SerializableModuleElaborator {
  val topName = "Clint"

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
    @arg(name = "parameter") parameter: os.Path,
    @arg(name = "target-dir") targetDir: os.Path = os.pwd
  ) = {
    val (firrtl, annos) = designImpl[Clint, ClintParameter](os.read(parameter))
    os.write.over(targetDir / s"${topName}.fir", firrtl)
    os.write.over(targetDir / s"${topName}.anno.json", annos)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args.toIndexedSeq)
} 