package zen

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util._
import chisel3.probe._
import utils._
import bus.simplebus._
import zen.frontend._
import zen.backend._
object ZenParameter {
  implicit def rwP: upickle.default.ReadWriter[ZenParameter] =
    upickle.default.macroRW
}

/** Parameter of [[Zen]] */
case class ZenParameter(
  ifuParameter: IFUParameter,
  icacheParameter: ICacheParameter,
  iduParameter: IDUParameter,
  isuParameter: ISUParameter,
  exuParameter: EXUParameter,
  wbuParameter: WBUParameter
) extends SerializableModuleParameter

/** Interface of [[Zen]]. */
class ZenInterface(parameter: ZenParameter) extends Bundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.ifuParameter.useAsyncReset) AsyncReset() else Bool())
  val imemOut = new SimpleBus()
  val dmemOut = new SimpleBus()
}

/** Hardware Implementation of [[Zen]] */
@instantiable
class Zen(val parameter: ZenParameter)
    extends FixedIORawModule(new ZenInterface(parameter))
    with SerializableModule[ZenParameter]
    with ImplicitClock
    with ImplicitReset {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = s"Zen"

  // frontend
  val ifu = Instantiate(new IFU(parameter.ifuParameter))
  val icache = Instantiate(new ICache()(parameter.icacheParameter))
  val idu = Instantiate(new IDU(parameter.iduParameter))
  // backend
  val isu = Instantiate(new ISU(parameter.isuParameter))
  val exu = Instantiate(new EXU(parameter.exuParameter))
  val wbu = Instantiate(new WBU(parameter.wbuParameter))

  // connect ifu and icache
  ifu.io.imem <> icache.io.in
  icache.io.out <> io.imemOut

  // connect dmem
  exu.io.dmem <> io.dmemOut

  // pip connect
  PipelineConnect(ifu.io.out, idu.io.in)
  PipelineConnect(idu.io.out, isu.io.in)
  PipelineConnect(isu.io.out, exu.io.in)
  PipelineConnect(exu.io.out, wbu.io.in)


  //connect wbu
  isu.io.wb <> wbu.io.wb
  isu.io.fwd <> exu.io.forward

  //connect targetpc
  ifu.io.correctedPC <> exu.io.targetPC

  //flush
  val flush = ifu.io.isFlush
  idu.io.isFlush := flush(0)
  isu.io.isFlush := flush(1)
  exu.io.isFlush := flush(2)
  wbu.io.isFlush := flush(3)

  //TODO: fence.i
  icache.io.fencei.valid := false.B

  //clock and reset
  ifu.io.clock := io.clock
  ifu.io.reset := io.reset
  icache.io.clock := io.clock
  icache.io.reset := io.reset
  idu.io.clock := io.clock
  idu.io.reset := io.reset
  isu.io.clock := io.clock
  isu.io.reset := io.reset
  exu.io.clock := io.clock
  exu.io.reset := io.reset
  wbu.io.clock := io.clock
  wbu.io.reset := io.reset
} 
