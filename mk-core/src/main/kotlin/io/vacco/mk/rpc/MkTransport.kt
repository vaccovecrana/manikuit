package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.storage.MkBlockCache
import io.vacco.mk.util.*
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

abstract class MkTransport(val config: MkConfig, private val blockCache: MkBlockCache):
    MkCachingTransport(config) {

  init {
    require(config.blockCacheLimit > 0)
    require(config.blockScanLimit > 0)
  }

  abstract fun getLatestBlockNumber(): Long
  abstract fun getBlock(height: Long): CgBlockSummary
  abstract fun getBlockDetail(summary: CgBlockSummary): CgBlockDetail
  abstract fun getChainType(): MkExchangeRate.CryptoCurrency
  abstract fun doCreate(): Pair<MkPayment, String>
  abstract fun getUrl(payment: MkPayment): String

  fun create(): MkPayment {
    val pData = doCreate()
    val key = GcmCrypto.generateKey(256)
    val encoded = GcmCrypto.encryptGcm(pData.second.toByteArray(), key)
    return pData.first
        .withCipherText(Base64.getEncoder().encodeToString(encoded.ciphertext))
        .withIv(Base64.getEncoder().encodeToString(encoded.iv))
        .withGcmKey(Base64.getEncoder().encodeToString(key))
  }

  fun decode(payment: MkPayment): String {
    requireNotNull(payment.cipherText)
    requireNotNull(payment.iv)
    requireNotNull(payment.gcmKey)
    val dec = Base64.getDecoder()
    return String(GcmCrypto.decryptGcm(
        Ciphertext(dec.decode(payment.cipherText),
            dec.decode(payment.iv)), dec.decode(payment.gcmKey)),
        Charsets.UTF_8)
  }

  override fun update() {
    purge()
    val utcCoff = blockScanCutOffSec()
    val blockList: MutableList<CgBlockSummary> = mutableListOf()
    val loc0 = blockCache.getLatestLocalBlockFor(getChainType())
    var rem0 = getLatestBlockNumber()
    if (rem0 >= loc0) {
      var blockSummary = getBlock(rem0)
      while(blockSummary.first.timeUtcSec >= utcCoff) {
        rem0 -= 1
        blockList.add(blockSummary)
        blockSummary = getBlock(rem0)
      }
    }
    if (blockList.isNotEmpty()) {
      blockCache.store(blockList
          .map(this::getBlockDetail)
          .map { bd -> CgBlockDetail(
              bd.first.copy(id = MurmurHash3.apply(bd.first.hash, bd.first.type)),
              bd.second
          )})
    }
  }

  override fun purge() = blockCache.purge(blockCacheCutOffSec(), getChainType())

  fun getPaymentsFor(address: String, type: MkExchangeRate.CryptoCurrency): List<MkPaymentRecord> =
      blockCache.getPaymentsFor(address, type)

  fun getStatus(payment: MkPaymentRecord, currentBlockHeight: Long): MkPaymentRecord.Status {
    return if (getBlockDelta(payment, currentBlockHeight) >= config.confirmationThreshold)
      MkPaymentRecord.Status.COMPLETE else MkPaymentRecord.Status.PENDING
  }

  fun getBlockDelta(payment: MkPaymentRecord, currentBlockHeight: Long): Long {
    return currentBlockHeight - payment.blockHeight
  }

  private fun blockScanCutOffSec(): Long =
      nowUtcSecMinus(config.blockScanLimit, config.blockScanLimitUnit)

  private fun blockCacheCutOffSec(): Long =
      nowUtcSecMinus(config.blockCacheLimit, ChronoUnit.valueOf(config.blockCacheLimitUnit.toString()))

  private fun nowUtc(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

  fun nowUtcSecMinus(amount: Long, unit: ChronoUnit): Long {
    val utcLimit = nowUtc().minus(amount, unit)
    return utcLimit.toEpochSecond()
  }
}
