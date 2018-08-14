package io.vacco.mk.rpc

import com.ifesdjeen.blomstre.BloomFilter
import io.vacco.mk.base.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.spi.MkBlockCache
import io.vacco.mk.util.*
import org.slf4j.*
import java.io.Closeable
import java.math.BigDecimal
import java.math.BigInteger
import java.time.*
import java.time.temporal.ChronoUnit

abstract class MkTransport(val config: MkConfig, private val blockCache: MkBlockCache):
    MkCachingTransport(config), Closeable {

  protected val accountDataSep = "::"
  protected val log: Logger = LoggerFactory.getLogger(javaClass)
  private var txAddressFilter: BloomFilter<MkPaymentRecord>? = null

  abstract fun computeFee(from: MkPaymentDetail, to: Collection<MkPaymentTarget>, txUnitFee: BigInteger): BigInteger
  abstract fun encodeAmount(amount: BigDecimal): String
  abstract fun decodeToUnit(rawAmount: String): BigInteger
  abstract fun doCreate(): Pair<String, String>
  abstract fun doBroadcast(source: MkPaymentDetail, targets: Collection<MkPaymentTarget>,
                           unitFee: BigInteger): Collection<MkPaymentTarget>

  abstract fun getLatestBlockNumber(): Long
  protected abstract fun doGetBlockDetail(height: Long): MkBlockDetail

  abstract fun getChainType(): MkExchangeRate.Crypto
  abstract fun getCoinPrecision(): Int
  abstract fun getFeeSplitMode(): MkSplit.FeeMode

  abstract fun getUrl(payment: MkPaymentDetail): String

  var onNewBlock: (block: MkBlockDetail) -> Unit = {}
  var onAddressMatch: (payment: MkPaymentRecord) -> Unit = {}

  init {
    require(config.blockCacheLimit > 0)
    require(config.blockScanLimit > 0)
    txAddressFilter = BloomFilter.makeFilter(MkAccountCodec::fingerPrintAddress,
        config.txFilterCapacity, config.txFilterMaxFalsePositiveProbability)
  }

  fun newBlock(blockDetail: MkBlockDetail) {
    if (log.isDebugEnabled) { log.debug("New [${blockDetail.first.type}] block with [${blockDetail.second.size}] transactions.") }
    if (blockDetail.second.isNotEmpty()) {
      wrap({
        blockCache.storeBlock(blockDetail.first)
        blockCache.storeRecords(blockDetail.second)
      }, "Block cache storage error.")
      wrap({ onNewBlock(blockDetail) }, "New block processing error. Verify listener implementation.")
      blockDetail.second
          .filter {
            if (log.isDebugEnabled) { log.debug(it.toString()) }
            txAddressFilter!!.isPresent(it)
          }.map(this::hash)
          .forEach{
            wrap({
              log.warn("Address notification match: [${it.address}]")
              onAddressMatch(it)
            }, "Address notification processing error.")
          }
    }
  }

  private fun wrap(action: () -> Unit, message: String) {
    try { action() }
    catch (e: Exception) { log.error(message, e) }
  }

  fun notifyOnAddress(pr: MkPaymentRecord) {
    requireNotNull(pr.type)
    requireNotNull(pr.address)
    require(pr.type === getChainType())
    log.warn("Address notification watch: [${pr.address}]")
    txAddressFilter!!.add(pr)
  }

  fun getBlockDetail(height: Long): MkBlockDetail {
    val bd = doGetBlockDetail(height)
    return bd.copy(second = bd.second.map(this::hash))
  }

  private fun hash(p: MkPaymentRecord): MkPaymentRecord {
    p.id = MkHashing.apply(p.type, p.address, p.amount, p.blockHeight, p.txId)
    return p
  }

  fun create(): MkAccount {
    val pData = doCreate()
    return MkAccountCodec.encodeRaw(pData.first, pData.second, getChainType())
  }

  fun broadcast(payment: MkPaymentDetail, targets: Collection<MkPaymentTarget>,
                txUnitFee: BigInteger, txFee: BigInteger): Collection<MkPaymentTarget> {
    val splitTargets = MkSplit.apply(decodeToUnit(payment.record.amount),
        txFee, getFeeSplitMode(), targets)
    return doBroadcast(payment, splitTargets, txUnitFee)
  }

  override fun update() {
    val utcCoff = blockScanCutOffSec()
    val blocks = mutableListOf<MkBlockDetail>()
    val localLatest = blockCache.getLatestLocalBlockFor(getChainType())
    if (getLatestBlockNumber() >= localLatest) {
      var remoteStart = getLatestBlockNumber()
      var block = getBlockDetail(remoteStart)
      while(true) {
        if (block.first.timeUtcSec <= utcCoff) break
        if (block.first.height <= localLatest) break
        blockCache.storeBlock(block.first)
        blocks.add(block)
        remoteStart -= 1
        block = getBlockDetail(remoteStart)
      }
    }
    blockCache.storeRecords(blocks.flatMap { it.second })
  }

  override fun purge() = blockCache.purge(blockCacheCutOffSec(), getChainType())

  fun getPaymentsFor(address: String): List<MkPaymentRecord> =
      blockCache.getPaymentsFor(address, getChainType())

  fun getStatus(payment: MkPaymentRecord, currentBlockHeight: Long): MkPaymentRecord.Status {
    val delta = getBlockDelta(payment, currentBlockHeight)
    if (delta >= config.confirmationThreshold) return MkPaymentRecord.Status.COMPLETE
    if (delta > 0 && delta < config.confirmationThreshold) return MkPaymentRecord.Status.PROCESSING
    return MkPaymentRecord.Status.PENDING
  }

  fun getBlockDelta(payment: MkPaymentRecord, currentBlockHeight: Long): Long {
    return currentBlockHeight - (payment.blockHeight - 1)
  }

  private fun blockScanCutOffSec(): Long = nowUtcSecMinus(config.blockScanLimit, config.blockScanLimitUnit)
  private fun blockCacheCutOffSec(): Long =
      nowUtcSecMinus(config.blockCacheLimit, ChronoUnit.valueOf(config.blockCacheLimitUnit.toString()))

  private fun nowUtc(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

  fun nowUtcSecMinus(amount: Long, unit: ChronoUnit): Long {
    val utcLimit = nowUtc().minus(amount, unit)
    return utcLimit.toEpochSecond()
  }
}
