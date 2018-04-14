package io.vacco.mk.util

import io.vacco.mk.base.MkBlock
import io.vacco.mk.base.MkExchangeRate
import io.vacco.mk.base.MkPaymentRecord
import io.vacco.mk.spi.MkBlockCache

open class NoOpBlockCache : MkBlockCache {

  override fun storeRecords(records: List<MkPaymentRecord>) {}

  override fun storeBlock(block: MkBlock) {}

  override fun getLatestLocalBlockFor(type: MkExchangeRate.Crypto): Long = 0

  override fun getPaymentsFor(address: String, type: MkExchangeRate.Crypto): List<MkPaymentRecord> = emptyList()

  override fun purge(cacheLimit: Long, type: MkExchangeRate.Crypto) {}
}
