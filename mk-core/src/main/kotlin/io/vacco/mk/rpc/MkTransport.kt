package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.storage.MkBlockCache
import io.vacco.mk.util.*
import org.slf4j.*
import java.io.Closeable
import java.math.BigInteger
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

abstract class MkTransport(val config: MkConfig, private val blockCache: MkBlockCache):
    MkCachingTransport(config), Closeable {

  init {
    require(config.blockCacheLimit > 0)
    require(config.blockScanLimit > 0)
  }

  protected val log: Logger = LoggerFactory.getLogger(javaClass)

  abstract fun decodeToUnit(rawAmount: String): BigInteger
  abstract fun doCreate(): Pair<String, String>
  abstract fun doBroadcast(source: MkPaymentDetail, targets: Collection<MkPaymentTarget>, unitFee: BigInteger): String

  abstract fun getLatestBlockNumber(): Long
  abstract fun getBlock(height: Long): MkBlockSummary
  abstract fun getBlockDetail(summary: MkBlockSummary): MkBlockDetail
  abstract fun getChainType(): MkExchangeRate.Crypto
  abstract fun getCoinPrecision(): Int
  abstract fun getFeeSplitMode(): MkSplit.FeeMode
  abstract fun getUrl(payment: MkPaymentDetail): String

  var onNewBlock: (block: MkBlockDetail) -> Unit = {}

  protected fun newBlock(blockDetail: MkBlockDetail) {
    blockCache.storeBlock(blockDetail.first)
    blockCache.storeRecords(blockDetail.second)
    onNewBlock(blockDetail)
  }

  fun create(): MkAccount {
    val pData = doCreate()
    return encodeRaw(pData.first, pData.second)
  }

  fun decode(account: MkAccount): String {
    requireNotNull(account.cipherText)
    requireNotNull(account.iv)
    requireNotNull(account.gcmKey)
    val dec = Base64.getDecoder()
    return String(GcmCrypto.decryptGcm(
        Ciphertext(dec.decode(account.cipherText),
            dec.decode(account.iv)), dec.decode(account.gcmKey)), Charsets.UTF_8)
  }

  fun encodeRaw(address: String, pData: String): MkAccount {
    requireNotNull(address)
    requireNotNull(pData)
    val key = GcmCrypto.generateKey(256)
    val encoded = GcmCrypto.encryptGcm(pData.toByteArray(), key)
    val b64Enc = Base64.getEncoder()
    val account = MkAccount(getChainType(), address,
        b64Enc.encodeToString(encoded.ciphertext),
        b64Enc.encodeToString(encoded.iv),
        b64Enc.encodeToString(key))
    if (log.isTraceEnabled) log.trace(account.toString())
    return account
  }

  fun broadcast(payments: Collection<MkPaymentDetail>,
                targets: Collection<MkPaymentTarget>, unitFee: BigInteger): Map<String, MkPaymentDetail> {
    val pairs = payments.map { pd0 ->
      val splitTargets = MkSplit.apply(decodeToUnit(pd0.record.amount),
          unitFee, getCoinPrecision(), getFeeSplitMode(), targets)
      val txId = doBroadcast(pd0, splitTargets, unitFee)
      (txId to pd0)
    }.toTypedArray()
    return mapOf(*pairs)
  }

  override fun update() {
    purge()
    val utcCoff = blockScanCutOffSec()
    val localLatest = blockCache.getLatestLocalBlockFor(getChainType())
    val remoteLatest = getLatestBlockNumber()
    val blockSummaries = mutableListOf<MkBlockSummary>()
    if (remoteLatest >= localLatest) {
      var remoteStart = remoteLatest
      var blockSummary = getBlock(remoteStart)
      while(blockSummary.first.timeUtcSec >= utcCoff) {
        blockCache.storeBlock(blockSummary.first)
        blockSummaries.add(blockSummary)
        remoteStart -= 1
        blockSummary = getBlock(remoteStart)
      }
    }
    blockSummaries.asSequence().map(this::getBlockDetail).map { it.second }
        .forEach { paymentList -> blockCache.storeRecords(paymentList) }
  }

  override fun purge() = blockCache.purge(blockCacheCutOffSec(), getChainType())

  fun getPaymentsFor(address: String): List<MkPaymentRecord> = blockCache.getPaymentsFor(address, getChainType())

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
