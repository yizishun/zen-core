package zen.frontend

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public, Instance, Instantiate}
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util._
import chisel3.probe._
import utils._
import bus.simplebus._

object ICacheParameter {
  implicit def rwP: upickle.default.ReadWriter[ICacheParameter] =
    upickle.default.macroRW
}

/** Parameter of [[ICache]] */
case class ICacheParameter(
  width: Int,
  useAsyncReset: Boolean,
  readOnly: Boolean,
  // total size = set * way * line size Byte
  totalSize: Int,
  way: Int,
  lineSize: Int,
  memParameter: MemWithPortParameter,
  addressSpace: AddressSpaceConfig
) extends SerializableModuleParameter {
  require(memParameter.way == way, "way of memParameter must be the same as way of ICache")
  require(memParameter.set == totalSize / (way * lineSize), "set of memParameter must be the same as totalSize of ICache")
}

sealed trait HasCacheCons {
  val parameter: ICacheParameter
  val wordSize = parameter.width / 8 // Byte = 1 Word
  val Sets = parameter.totalSize / (parameter.way * parameter.lineSize)
  val LineSize = parameter.lineSize // Byte
  val LineBeats = LineSize / wordSize // Word
  val BeatSize = parameter.width // Bit
  val Ways = parameter.way

  val OffsetBits = log2Ceil(LineSize) // = WordIndexBits + ByteIndexBits
  val IndexBits = log2Ceil(Sets)
  val WordIndexBits = log2Ceil(LineBeats)
  val ByteIndexBits = log2Ceil(wordSize)
  val TagBits = parameter.width - OffsetBits - IndexBits

  // tag | index | (wordIndex | byteIndex)
  def addrDecode = new Bundle {
    val tag = UInt(TagBits.W)
    val index = UInt(IndexBits.W) // set index
    val wordIndex = UInt(WordIndexBits.W) // index in a line by word
    val byteIndex = UInt(ByteIndexBits.W) // byte offset in a word
  }

  def getMetaIdx(addr: UInt) = addr.asTypeOf(addrDecode).index // set
  def getDataIdx(addr: UInt) = Cat(addr.asTypeOf(addrDecode).index, addr.asTypeOf(addrDecode).wordIndex) // wordIndex

  def isSameWord(addr1: UInt, addr2: UInt) = (addr1 >> ByteIndexBits) === (addr2 >> ByteIndexBits)
  def isSetConflict(addr1: UInt, addr2: UInt) = getMetaIdx(addr1) === getMetaIdx(addr2)

  // 找到第一个invalid的way，返回one-hot编码
  def findFirstInvalidWay(metaReadWay: Vec[MetaBundle]): (Bool, Vec[Bool]) = {
    val invalidWays = VecInit(metaReadWay.map(!_.valid))
    val hasInvalidWay = invalidWays.asUInt.orR
    val firstInvalidWayOH = PriorityEncoderOH(invalidWays.asUInt)
    (hasInvalidWay, VecInit(firstInvalidWayOH.asBools))
  }
}

sealed abstract class CacheBundle(implicit val parameter: ICacheParameter) extends Bundle with HasCacheCons

sealed class MetaBundle(implicit override val parameter: ICacheParameter) extends CacheBundle {
  val tag = UInt(TagBits.W)
  val valid = Bool()
}

sealed class DataBundle(implicit override val parameter: ICacheParameter) extends CacheBundle {
  val data = Vec(LineBeats, UInt(parameter.width.W))

}

/** Interface of [[ICache]]. */
class ICacheInterface(implicit override val parameter: ICacheParameter) extends CacheBundle {
  val clock  = Input(Clock())
  val reset  = Input(if (parameter.useAsyncReset) AsyncReset() else Bool())
  val in = Flipped(new SimpleBus())
  val out = new SimpleBus()
  val fencei = Flipped(Decoupled())
}

/** Hardware Implementation of [[ICache]] */
//yosys:
@instantiable
class ICache(implicit val parameter: ICacheParameter)
    extends FixedIORawModule(new ICacheInterface)
    with SerializableModule[ICacheParameter]
    with ImplicitClock
    with ImplicitReset
    with HasCacheCons {
  override protected def implicitClock: Clock = io.clock
  override protected def implicitReset: Reset = io.reset
  override val desiredName: String = s"ICache"

  val metaMem = MemWithPort(parameter.memParameter, new MetaBundle)
  val dataMem = MemWithPort(parameter.memParameter, new DataBundle)
  val isInSdram = AddressSpace(parameter.addressSpace).isInRegion(io.in.req.bits.addr, "SDRAM")
  metaMem.clock := io.clock
  dataMem.clock := io.clock
  metaMem.reset := io.reset
  dataMem.reset := io.reset

  // fencei控制逻辑
  val fencing = io.fencei.valid
  val fenceCounter = Counter(Sets)
  
  // 计数器控制
  io.fencei.ready := false.B
  when(fencing) { io.fencei.ready := fenceCounter.inc() } //when wrap, ready is high

  //although it is multiple cycle, but i assume the input from IFU is stable, so i can save 32 bits reg area
  //or you can use reg and name the same name as below
  val (addr, cmd, size, _, _) = io.in.req.bits.get()
  if(parameter.readOnly) assert(cmd === SimpleBusCmd.read || cmd === SimpleBusCmd.readBurst)

  //check
  val metaReadWay = metaMem.read(getDataIdx(addr), 0)
  val hitOneHot = VecInit(metaReadWay.map(meta => meta.valid && meta.tag === addr.asTypeOf(addrDecode).tag)).asUInt
  val uncached = !isInSdram
  val hit = hitOneHot.orR
  val miss = !hit
  
  //interactive with in and out
  val handshake = Wire(Vec(4, Bool()))
  val finish = handshake(1)
  val resetHandshake = WireInit(Bool(), finish)
  // 在fencing时阻塞新请求
  handshake(0) := Handshake.slave(io.in.req, !fencing, resetHandshake)
  handshake(1) := Handshake.master(io.in.resp, handshake(0) && (hit || (uncached && handshake(3))), resetHandshake)
  handshake(2) := Handshake.master(io.out.req, handshake(0) && (miss || uncached), resetHandshake)
  handshake(3) := Handshake.simpleBusSlave(io.out.resp, handshake(2), resetHandshake, burst = uncached)

  //data path
  io.out.req.bits.send(
    addr = addr,
    cmd = Mux(uncached, SimpleBusCmd.read, SimpleBusCmd.readBurst),
    size = size,
    wdata = 0.U,
    wmask = 0.U
  )
  //get data
  val (rdata, rcmd) = io.out.resp.bits.get()
  //get way mask
  val victimWayMask = if(parameter.way > 1) VecInit((1.U << LFSR(log2Up(Ways))).asBools) else VecInit(true.B)
  val (hasInvalidWay, firstInvalidWayOH) = findFirstInvalidWay(metaReadWay)
  val wayMask = Mux(hasInvalidWay, firstInvalidWayOH, victimWayMask)
  //write to data mem
  val wdata = Wire(new DataBundle())
  wdata.data := DontCare
  when(io.out.resp.fire && !uncached) {
    wdata.data(Counter(io.out.resp.fire, LineBeats)._1) := rdata
  }
  dataMem.write(
    address = getDataIdx(addr),
    data = wdata,
    waymask = wayMask,
    enable = io.out.resp.fire && !uncached,
    portIdx = 0
  )
  //write to meta mem
  val meta = Wire(new MetaBundle())
  meta.tag := Mux(fencing, 0.U, addr.asTypeOf(addrDecode).tag)  // fencing时tag可以是任意值
  meta.valid := !fencing  // fencing时直接写无效
  
  // fence.i复用metaMem写口
  val metaWriteAddr = Mux(fencing, fenceCounter.value, getDataIdx(addr))
  val metaWriteWaymask = Mux(fencing, VecInit(Seq.fill(Ways)(true.B)), wayMask)
  val metaWriteEnable = Mux(fencing, true.B, io.out.resp.fire && !uncached)
  
  metaMem.write(
    address = metaWriteAddr,
    data = meta,
    waymask = metaWriteWaymask,
    enable = metaWriteEnable,
    portIdx = 0
  )
  //write the rdata to the out.resp
  val respData = Wire(UInt(parameter.width.W))
  val wayData = Wire(new DataBundle())
  val dataReadWay = dataMem.read(getDataIdx(addr), 0)
  wayData := dataReadWay(PriorityEncoder(hitOneHot))
  respData := Mux(uncached, rdata, wayData.data(addr.asTypeOf(addrDecode).wordIndex))
  io.in.resp.bits.send(
    rdata = respData,
    cmd = SimpleBusCmd.readLast,
  )

}
