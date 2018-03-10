package io.vacco.mk.storage

import com.onyx.persistence.manager.PersistenceManager
import com.onyx.persistence.query.*
import io.vacco.mk.base.*
import io.vacco.mk.util.MurmurHash3
import org.slf4j.*

class MkBlockCache(private val manager: PersistenceManager) {

  private val log: Logger = LoggerFactory.getLogger(this.javaClass)

  fun storeRecords(records: List<MkPaymentRecord>) = manager.saveEntities(
      records.map { r0 -> r0.copy(id = MurmurHash3.apply(
          r0.address, r0.amount, r0.blockHeight, r0.txId
      )) }
  )

  fun storeBlock(block: MkBlock) = manager.saveEntity(block.copy(
      id = MurmurHash3.apply(block.height, block.hash, block.type)))

  fun getLatestLocalBlockFor(type: MkAccount.Crypto): Long {
    val maxCol = max("height")
    val row: Map<String, Any> = manager.select(maxCol)
        .from(MkBlock::class).where("type" eq type).first()
    val result: Long? = row[maxCol] as Long?
    return result ?: 0
  }

  fun getPaymentsFor(address: String, type: MkAccount.Crypto): List<MkPaymentRecord> {
    return manager.from(MkPaymentRecord::class)
        .where("address" eq address)
        .and("type" eq type).lazy()
  }

  fun purge(cacheLimit: Long, type: MkAccount.Crypto) {
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
