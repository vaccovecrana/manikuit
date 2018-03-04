package unit

import com.onyx.persistence.factory.impl.EmbeddedPersistenceManagerFactory
import com.onyx.persistence.manager.PersistenceManager
import com.onyx.persistence.query.from
import io.vacco.mk.base.MkPaymentRecord
import io.vacco.mk.rpc.BitcoindTransport
import io.vacco.mk.storage.MkBlockCache
import j8spec.J8Spec.*
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import java.time.temporal.ChronoUnit
import com.onyx.persistence.query.*
import io.vacco.mk.base.MkExchangeRate
import io.vacco.mk.config.MkConfig
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class BitcoindTransportSpec {

  private val factory = EmbeddedPersistenceManagerFactory("/tmp/${this.javaClass.simpleName}")
  private var manager: PersistenceManager? = null
  private var btc: BitcoindTransport? = null
  private val tx40Min: MutableList<MkPaymentRecord> = ArrayList()
  private val tx40MinWithStatus:MutableList<Pair<MkPaymentRecord, MkPaymentRecord.Status>> = ArrayList()
  private var testAddress: String? = null

  init {
    beforeAll {
      factory.initialize()
      manager = factory.persistenceManager
      val cfg = MkConfig(6, 1, ChronoUnit.HOURS, 5, TimeUnit.SECONDS)
      cfg.rootUrl = "http://127.0.0.1:18332"
      cfg.username = "gopher"
      cfg.password = "omglol"
      btc = BitcoindTransport(cfg, MkBlockCache(manager!!))
    }
    it("Can update the BTC cache.") { btc!!.update() }
    it("Can find transactions recorded in the last 40 minutes.") {
      val utc15MinAgo = btc!!.nowUtcSecMinus(40, ChronoUnit.MINUTES)
      val tx = manager!!.from(MkPaymentRecord::class)
          .where("timeUtcSec" gte utc15MinAgo)
          .list<MkPaymentRecord>()
      assertTrue(tx.isNotEmpty())
      tx40Min.addAll(tx)
    }
    it("Can map all transactions to their corresponding status, according to the latest block height.") {
      val blockNow = btc!!.getLatestBlockNumber()
      tx40MinWithStatus.addAll(tx40Min.map {
        payment -> Pair(payment, btc!!.getStatus(payment, blockNow))
      })
      tx40MinWithStatus.forEach { p0 ->
        val blockDelta = btc!!.getBlockDelta(p0.first, blockNow)
        val confThreshold = btc!!.config.confirmationThreshold
        if (p0.second == MkPaymentRecord.Status.COMPLETE) { assertTrue(blockDelta >= confThreshold) }
        else { assertTrue(blockDelta < confThreshold) }
      }
      testAddress = tx40Min[0].address
      assertNotNull(testAddress)
    }
    it("Can get all transactions for a particular address.") {
      val addrTx = btc!!.getPaymentsFor(testAddress!!, MkExchangeRate.CryptoCurrency.BTC)
      assertTrue(addrTx.isNotEmpty())
    }
    it("Can purge the cache for records older than 5 seconds.") {
      btc!!.purgeCache()
      val tx = manager!!.from(MkPaymentRecord::class).list<MkPaymentRecord>()
      assertTrue(tx.isEmpty())
    }
    it("Can create a new payment, along with a backing account, using 2 of 3 secret shares.") {
      val payment = btc!!.create(null, 3, 2);
      assertNotNull(payment)
      assertNotNull(payment.secretParts)
      assertTrue(payment.secretParts.size == 3)
    }
  }
}