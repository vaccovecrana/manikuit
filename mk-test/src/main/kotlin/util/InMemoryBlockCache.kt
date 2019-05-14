package util

import io.vacco.mk.base.MkBlock
import io.vacco.mk.base.MkExchangeRate
import io.vacco.mk.base.MkPaymentRecord
import io.vacco.mk.spi.MkBlockCache
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class InMemoryBlockCache : MkBlockCache {

  var fakeId = 0L
  val paymentById = HashMap<Long, MkPaymentRecord>()
  val paymentByAddress = HashMap<String, MutableList<MkPaymentRecord>>()
  val blockByHeight = TreeMap<Long, MkBlock>()

  override fun storeRecords(records: List<MkPaymentRecord>) {
    records.forEach {
      fakeId++
      it.pid = fakeId
      paymentById.put(it.pid, it)
      var addrPy = paymentByAddress[it.address]
      if (addrPy == null) {
        addrPy = ArrayList()
        paymentByAddress[it.address] = addrPy
      }
      addrPy.add(it)
    }
    val rec = records.map { it.pid to it }.toTypedArray()
    paymentById.putAll(mapOf(*rec))
  }

  override fun storeBlock(block: MkBlock) {
    blockByHeight[block.height] = block
  }

  override fun getLatestLocalBlockFor(type: MkExchangeRate.Crypto): Long {
    val blockH = if (blockByHeight.isEmpty()) 0 else
      blockByHeight.keys.reversed().iterator().next()
    return blockH
  }

  override fun getPaymentsFor(address: String, type: MkExchangeRate.Crypto):
      List<MkPaymentRecord> = paymentByAddress[address] ?: emptyList()

  override fun purge(cacheLimit: Long, type: MkExchangeRate.Crypto) {
    val toRemove = paymentById.values
        .filter { it.type === type }
        .filter { it.timeUtcSec <= cacheLimit }
    toRemove.forEach {
      paymentById.remove(it.pid)
      paymentByAddress.remove(it.address)
    }
  }
}
