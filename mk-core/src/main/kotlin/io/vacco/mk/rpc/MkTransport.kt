package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.storage.MkBlockCache
import io.vacco.mk.util.MurmurHash3
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import net.tribe7.reason.Check.*

abstract class MkTransport(val config: MkConfig,
                           private val blockCache: MkBlockCache) : RpcTransport(config) {

  init {
    isTrue(config.blockCacheLimit > 0)
    isTrue(config.blockScanLimit > 0)
    validTimeUnit(config.blockCacheLimitUnit)
    validChronoUnit(config.blockScanLimitUnit)
  }

  abstract fun getLatestBlockNumber(): Long
  abstract fun getBlock(height: Long): CgBlockSummary
  abstract fun getBlockDetail(summary: CgBlockSummary): CgBlockDetail
  abstract fun getChainType(): MkExchangeRate.CryptoCurrency
  abstract fun create(rawSecret: String?, secretParts: Int, secretRequired: Int): MkPayment

  fun update() {
    purgeCache()
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

  fun purgeCache() = blockCache.purge(blockCacheCutOffSec(), getChainType())

  fun getPaymentsFor(address: String, type: MkExchangeRate.CryptoCurrency): List<MkPaymentRecord> =
      blockCache.getPaymentsFor(address, type)

  fun getStatus(payment: MkPaymentRecord, currentBlockHeight: Long): MkPaymentRecord.Status {
    return if (getBlockDelta(payment, currentBlockHeight) >= config.confirmationThreshold)
      MkPaymentRecord.Status.COMPLETE else MkPaymentRecord.Status.PENDING
  }

  fun getBlockDelta(payment: MkPaymentRecord, currentBlockHeight: Long): Long {
    return currentBlockHeight - payment.blockHeight
  }

  private fun validChronoUnit(cUnit: String) = ChronoUnit.valueOf(cUnit)

  private fun validTimeUnit(cacheUnit: MkConfig.BlockCacheLimitUnit) = TimeUnit.valueOf(cacheUnit.toString())

  private fun blockScanCutOffSec(): Long =
      nowUtcSecMinus(config.blockScanLimit, ChronoUnit.valueOf(config.blockScanLimitUnit))

  private fun blockCacheCutOffSec(): Long =
      nowUtcSecMinus(config.blockCacheLimit, ChronoUnit.valueOf(config.blockCacheLimitUnit.toString()))

  private fun nowUtc(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)

  fun nowUtcSecMinus(amount: Long, unit: ChronoUnit): Long {
    val utcLimit = nowUtc().minus(amount, unit)
    return utcLimit.toEpochSecond()
  }
}