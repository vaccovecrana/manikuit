package unit

import com.onyx.persistence.factory.impl.EmbeddedPersistenceManagerFactory
import com.onyx.persistence.manager.PersistenceManager
import com.onyx.persistence.query.from
import com.onyx.persistence.query.gte
import io.vacco.mk.base.MkConfig
import io.vacco.mk.base.MkExchangeRate
import io.vacco.mk.base.MkPaymentRecord
import io.vacco.mk.rpc.GethTransport
import io.vacco.mk.storage.MkBlockCache
import j8spec.J8Spec
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.Assert
import org.junit.runner.RunWith
import java.time.temporal.ChronoUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class GethTransportSpec {

  private val factory = EmbeddedPersistenceManagerFactory("/tmp/${this.javaClass.simpleName}")
  private var manager: PersistenceManager? = null
  private var eth: GethTransport? = null
  private val tx40Min: MutableList<MkPaymentRecord> = ArrayList()
  private val tx40MinWithStatus:MutableList<Pair<MkPaymentRecord, MkPaymentRecord.Status>> = ArrayList()
  private var testAddress: String? = null

  init {
    J8Spec.beforeAll {
      factory.initialize()
      manager = factory.persistenceManager
      eth = GethTransport(
          MkConfig().withRootUrl("http://127.0.0.1:8545")
              .withConfirmationThreshold(12)
              .withBlockCacheLimit(5)
              .withBlockCacheLimitUnit(MkConfig.BlockCacheLimitUnit.SECONDS)
              .withBlockScanLimit(1)
              .withBlockScanLimitUnit("HOURS"),
          MkBlockCache(manager!!)
      )
    }
    J8Spec.it("Can update the ETH cache.") { eth!!.update() }
    J8Spec.it("Can find transactions recorded in the last 40 minutes.") {
      val utc15MinAgo = eth!!.nowUtcSecMinus(40, ChronoUnit.MINUTES)
      val tx = manager!!.from(MkPaymentRecord::class)
          .where("timeUtcSec" gte utc15MinAgo)
          .list<MkPaymentRecord>()
      Assert.assertTrue(tx.isNotEmpty())
      tx40Min.addAll(tx)
    }
    J8Spec.it("Can map all transactions to their corresponding status, according to the latest block height.") {
      val blockNow = eth!!.getLatestBlockNumber()
      tx40MinWithStatus.addAll(tx40Min.map { payment ->
        Pair(payment, eth!!.getStatus(payment, blockNow))
      })
      tx40MinWithStatus.forEach { p0 ->
        val blockDelta = eth!!.getBlockDelta(p0.first, blockNow)
        val confThreshold = eth!!.config.confirmationThreshold
        if (p0.second == MkPaymentRecord.Status.COMPLETE) {
          Assert.assertTrue(blockDelta >= confThreshold)
        } else {
          Assert.assertTrue(blockDelta < confThreshold)
        }
      }
      testAddress = tx40Min[0].address
      Assert.assertNotNull(testAddress)
    }
    J8Spec.it("Can get all transactions for a particular address.") {
      val addrTx = eth!!.getPaymentsFor(testAddress!!, MkExchangeRate.CryptoCurrency.ETH)
      Assert.assertTrue(addrTx.isNotEmpty())
    }
    J8Spec.it("Can purge the cache for records older than 5 seconds.") {
      eth!!.purgeCache()
      val tx = manager!!.from(MkPaymentRecord::class).list<MkPaymentRecord>()
      Assert.assertTrue(tx.isEmpty())
    }
    J8Spec.it("Can create a new payment, along with a backing account, using 2 of 3 secret shares.") {
      val payment = eth!!.create("iamthegreatgopher", 3, 2)
      Assert.assertNotNull(payment)
      Assert.assertNotNull(payment.secretParts)
      Assert.assertTrue(payment.secretParts.size == 3)
    }
  }
}