package io.vacco.mk.spi

import io.vacco.mk.base.MkBlock
import io.vacco.mk.base.MkExchangeRate
import io.vacco.mk.base.MkPaymentRecord

/**
 * General definitions for a block chain hot cache.
 */
interface MkBlockCache {

  /**
   * Persists an incoming payment record update batch.
   *
   * @param records the list of records to persist.
   */
  fun storeRecords(records: List<MkPaymentRecord>)

  /**
   * Persist information about a new incoming block.
   *
   * @param block the target block.
   */
  fun storeBlock(block: MkBlock)

  /**
   * Retrieves the latest locally saved block number
   * for a given crypto currency type.
   * @param type a supported crypto currency type.
   */
  fun getLatestLocalBlockFor(type: MkExchangeRate.Crypto): Long

  /**
   * Retrieves the last known set of payments directed to a
   * particular crypto currency address.
   *
   * @param address the target address.
   * @param type the crypto currency type of the address.
   */
  fun getPaymentsFor(address: String, type: MkExchangeRate.Crypto): List<MkPaymentRecord>

  /**
   * Purges records older than the specified UTC second timestamp
   * for a particular crypto currency type.
   *
   * @param cacheLimit a UTC timestamp in seconds.
   * @param type a crypto currency type.
   */
  fun purge(cacheLimit: Long, type: MkExchangeRate.Crypto)

}