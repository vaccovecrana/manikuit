package io.vacco.mk.storage

import com.onyx.persistence.manager.PersistenceManager
import com.onyx.persistence.query.*
import io.vacco.mk.base.*
import io.vacco.mk.rpc.CgBlockDetail
import org.slf4j.*

class MkBlockCache(private val manager: PersistenceManager) {

  private val log: Logger = LoggerFactory.getLogger(this.javaClass)

  fun store(blockRecords: List<CgBlockDetail>) {
    manager.saveEntities(blockRecords.map { p0 -> p0.first })
    manager.saveEntities(blockRecords.flatMap { r0 -> r0.second })
  }

  fun getLatestLocalBlockFor(type: MkExchangeRate.CryptoCurrency): Long {
    val maxCol = max("height")
    val row: Map<String, Any> = manager.select(maxCol)
        .from(MkBlock::class).where("type" eq type)
        .first()
    val result: Long? = row[maxCol] as Long?
    return result ?: 0
  }

  fun getPaymentsFor(address: String, type: MkExchangeRate.CryptoCurrency): List<MkPaymentRecord> {
    return manager.from(MkPaymentRecord::class)
        .where("address" eq address)
        .and("type" eq type).lazy()
  }

  fun purge(cacheLimit: Long, type: MkExchangeRate.CryptoCurrency) {
    val purgedPayments = manager.from(MkPaymentRecord::class)
        .where("type" eq type)
        .and("timeUtcSec" lte cacheLimit).delete()
    val purgedBlocks = manager.from(MkBlock::class)
        .where("type" eq type)
        .and("timeUtcSec" lte cacheLimit).delete()
    if (log.isDebugEnabled) {
      log.debug("Cache limit: [$cacheLimit] Purged: [$purgedBlocks] blocks, [$purgedPayments] transactions.")
    }
  }
}
