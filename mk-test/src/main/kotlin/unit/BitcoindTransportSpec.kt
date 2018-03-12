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
import io.vacco.mk.config.MkConfig
import org.junit.Assert.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class BitcoindTransportSpec {

  private val log: Logger = LoggerFactory.getLogger(this.javaClass)

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
      val cfg = MkConfig(6, 1, ChronoUnit.HOURS, 10, TimeUnit.SECONDS)
      cfg.pubSubUrl = "tcp://127.0.0.1:28332"
      cfg.rootUrl = "http://127.0.0.1:18332"
      cfg.username = "gopher"
      cfg.password = "omglol"
      cfg.connectionPoolSize = 8
      btc = BitcoindTransport(cfg, MkBlockCache(manager!!))
    }

    it("Can create a new payment, along with a backing account.") {
      val payment = btc!!.create()
      assertNotNull(payment)
      assertNotNull(payment.cipherText)
      assertNotNull(payment.iv)
      assertNotNull(payment.gcmKey)
      val keyData = btc!!.decode(payment)
      assertNotNull(keyData)
      log.info(keyData)
    }

    it("Can verify transaction outputs against the RPC client.") {
      val bd0 = btc!!.getBlockDetail(btc!!.getBlock(1287687))
      bd0.second.filter { pr -> pr.outputIdx > 2 }
          .map { pr0 -> btc!!.getTxOut(pr0.txId, pr0.outputIdx) }
          .forEach { txo -> log.info(txo.toString()) }
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
      val addrTx = btc!!.getPaymentsFor(testAddress!!)
      assertTrue(addrTx.isNotEmpty())
    }
    it("Can purge the cache for records older than 5 seconds.") {
      btc!!.purge()
      val tx = manager!!.from(MkPaymentRecord::class).list<MkPaymentRecord>()
      assertTrue(tx.isEmpty())
    }
  }
}