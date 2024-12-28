package device

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util._
import bus.simplebus._
import utils._

object ClintParameter {
  implicit def rwP: upickle.default.ReadWriter[ClintParameter] =
    upickle.default.macroRW
}

/** CLINT的参数配置 */
case class ClintParameter(
    mtimeWidth: Int = 64,           // 数据位宽
    useAsyncReset: Boolean,    // 是否使用异步复位
) extends SerializableModuleParameter

/** CLINT的接口定义 */
class ClintInterface(val parameter: ClintParameter) extends Bundle {
  val clock = Input(Clock())
  val reset = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val in = Flipped(new SimpleBus())
}

/** CLINT硬件实现 */
@instantiable
class Clint(implicit val parameter: ClintParameter)
    extends FixedIORawModule(new ClintInterface(parameter))
    with SerializableModule[ClintParameter]
    with ImplicitClock
    with ImplicitReset {

  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = "Clint"

  val mtime = RegInit(0.U(parameter.mtimeWidth.W))
  mtime := mtime + 1.U

  //handshake
  io.in.resp.valid := io.in.resp.ready
  io.in.req.ready := io.in.req.valid

  //data Path
  val data = Mux(io.in.req.bits.addr(2), mtime(63, 32), mtime(31, 0))
  io.in.resp.bits.send(rdata = data, cmd = SimpleBusCmd.readLast)
}

