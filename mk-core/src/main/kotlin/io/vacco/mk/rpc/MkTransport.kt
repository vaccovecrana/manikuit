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

  protected val log: Logger = LoggerFactory.getLogger(javaClass)
  private var txAddressFilter: BloomFilter<MkPaymentRecord>? = null

  abstract fun encodeAmount(amount: BigDecimal): String
  abstract fun decodeToUnit(rawAmount: String): BigInteger
  abstract fun doCreate(): Pair<String, String>
  abstract fun doBroadcast(source: MkPaymentDetail, targets: Collection<MkPaymentTarget>,
                           unitFee: BigInteger): Collection<MkPaymentTarget>

  abstract fun getLatestBlockNumber(): Long
  abstract fun getBlock(height: Long): MkBlockSummary

  protected abstract fun doGetBlockDetail(summary: MkBlockSummary): MkBlockDetail

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
        config.txFilterCapacity, 0.001)
  }

  fun newBlock(blockDetail: MkBlockDetail) {
    try {
      if (blockDetail.second.isNotEmpty()) {
        blockCache.storeBlock(blockDetail.first)
        blockCache.storeRecords(blockDetail.second)
        onNewBlock(blockDetail)
        blockDetail.second
            .filter { txAddressFilter!!.isPresent(it) }
            .forEach{ onAddressMatch(it) }
      }
    } catch (e: Exception) {
      log.error("Unable to complete new block notifications. Please verify storage/listener implementations.", e)
    }
  }

  fun notifyOnAddress(pr: MkPaymentRecord) {
    requireNotNull(pr.type)
    requireNotNull(pr.address)
    require(pr.type === getChainType())
    txAddressFilter!!.add(pr)
  }

  fun getBlockDetail(summary: MkBlockSummary): MkBlockDetail {
    val bd = doGetBlockDetail(summary)
    return bd.copy(second = bd.second.map {
      it.copy(id = MurmurHash3.apply(it.type, it.address, it.amount, it.blockHeight, it.txId))
    })
  }

  fun create(): MkAccount {
    val pData = doCreate()
    return MkAccountCodec.encodeRaw(pData.first, pData.second, getChainType())
  }

  fun broadcast(payment: MkPaymentDetail, targets: Collection<MkPaymentTarget>,
                unitFee: BigInteger): Collection<MkPaymentTarget> {
    val splitTargets = MkSplit.apply(decodeToUnit(payment.record.amount),
        unitFee, getFeeSplitMode(), targets)
    return doBroadcast(payment, splitTargets, unitFee)
  }

  override fun update() {
    val utcCoff = blockScanCutOffSec()
    val blockSummaries = mutableListOf<MkBlockSummary>()
    val localLatest = blockCache.getLatestLocalBlockFor(getChainType())
    if (getLatestBlockNumber() >= localLatest) {
      var remoteStart = getLatestBlockNumber()
      var blockSummary = getBlock(remoteStart)
      while(true) {
        if (blockSummary.first.timeUtcSec <= utcCoff) break
        if (blockSummary.first.height <= localLatest) break
        blockCache.storeBlock(blockSummary.first)
        blockSummaries.add(blockSummary)
        remoteStart -= 1
        blockSummary = getBlock(remoteStart)
      }
    }
    blockSummaries.asSequence()
        .map(this::getBlockDetail).map { it.second }
        .forEach { paymentList -> blockCache.storeRecords(paymentList) }
  }

  override fun purge() = blockCache.purge(blockCacheCutOffSec(), getChainType())

  fun getPaymentsFor(address: String): List<MkPaymentRecord> =
      blockCache.getPaymentsFor(address, getChainType())

  fun getStatus(payment: MkPaymentRecord, currentBlockHeight: Long): MkPaymentRecord.Status {
    return if (getBlockDelta(payment, currentBlockHeight) >= config.confirmationThreshold)
      MkPaymentRecord.Status.COMPLETE else MkPaymentRecord.Status.PENDING
  }

  fun getBlockDelta(payment: MkPaymentRecord, currentBlockHeight: Long): Long {
    return currentBlockHeight - payment.blockHeight
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
