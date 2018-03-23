package unit

import com.onyx.persistence.factory.impl.EmbeddedPersistenceManagerFactory
import com.onyx.persistence.manager.PersistenceManager
import com.onyx.persistence.query.*
import io.vacco.mk.base.MkPaymentRecord
import io.vacco.mk.config.MkConfig
import io.vacco.mk.rpc.ParityTransport
import io.vacco.mk.storage.MkBlockCache
import j8spec.J8Spec.*
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.slf4j.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class ParityTransportSpec {

  private val log:Logger = LoggerFactory.getLogger(this.javaClass)

  private val factory = EmbeddedPersistenceManagerFactory("/tmp/${this.javaClass.simpleName}")
  private var manager: PersistenceManager? = null
  private var eth: ParityTransport? = null
  private val tx40Min: MutableList<MkPaymentRecord> = ArrayList()
  private val tx40MinWithStatus:MutableList<Pair<MkPaymentRecord, MkPaymentRecord.Status>> = ArrayList()
  private var testAddress: String? = null

  init {
    beforeAll {
      factory.initialize()
      manager = factory.persistenceManager
      val cfg = MkConfig(12,
          6, ChronoUnit.HOURS, 20, TimeUnit.MINUTES)
      cfg.pubSubUrl = "ws://127.0.0.1:8546"
      cfg.rootUrl = "http://127.0.0.1:8545"
      eth = ParityTransport(cfg, MkBlockCache(manager!!))
    }
    it("Can update the ETH cache.") { eth!!.update() }
    it("Can skip ETH cache update if it's up to date.") { eth!!.update() }
    it("Can find transactions recorded in the last 4 hours.") {
      val utc4HrAgo = eth!!.nowUtcSecMinus(4, ChronoUnit.HOURS)
      val tx = manager!!.from(MkPaymentRecord::class)
          .where("timeUtcSec" gte utc4HrAgo)
          .list<MkPaymentRecord>()
      assertTrue(tx.isNotEmpty())
      tx40Min.addAll(tx)
    }
    it("Can map all transactions to their corresponding status, according to the latest block height.") {
      val blockNow = eth!!.getLatestBlockNumber()
      tx40MinWithStatus.addAll(tx40Min.map { payment ->
        Pair(payment, eth!!.getStatus(payment, blockNow))
      })
      tx40MinWithStatus.forEach { p0 ->
        val blockDelta = eth!!.getBlockDelta(p0.first, blockNow)
        val confThreshold = eth!!.config.confirmationThreshold
        if (p0.second == MkPaymentRecord.Status.COMPLETE) {
          assertTrue(blockDelta >= confThreshold)
        } else {
          assertTrue(blockDelta < confThreshold)
        }
      }
      testAddress = tx40Min[0].address
      assertNotNull(testAddress)
    }
    it("Can get all transactions for a particular address.") {
      val addrTx = eth!!.getPaymentsFor(testAddress!!)
      assertTrue(addrTx.isNotEmpty())
    }
    it("Can purge the cache for records older than 5 seconds.") { eth!!.purge() }
    it("Can create a new payment, along with a backing account.") {
      val payment = eth!!.create()
      assertNotNull(payment)
      assertNotNull(payment.gcmKey)
      val keyData = eth!!.decode(payment)
      assertNotNull(keyData)
      log.info(keyData)
    }
    it("Opens a new websocket Parity connection and processes incoming messages") {
      Thread.sleep(30_000)
      eth?.close()
    }
  }
}
