package unit

import io.vacco.mk.base.MkAccount
import io.vacco.mk.base.MkPaymentDetail
import io.vacco.mk.base.MkPaymentRecord
import io.vacco.mk.config.MkConfig
import io.vacco.mk.rpc.MkAccountCodec
import io.vacco.mk.rpc.ParityTransport
import j8spec.J8Spec.*
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.slf4j.*
import util.InMemoryBlockCache
import java.math.BigDecimal
import java.math.BigInteger
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class ParityTransportSpec {

  private val log:Logger = LoggerFactory.getLogger(this.javaClass)

  private var eth: ParityTransport? = null
  private val ethCache: InMemoryBlockCache = InMemoryBlockCache()
  private val tx40Min: MutableList<MkPaymentRecord> = ArrayList()
  private val tx40MinWithStatus:MutableList<Pair<MkPaymentRecord, MkPaymentRecord.Status>> = ArrayList()
  private var testAddress: String? = null

  init {
    beforeAll {
      val cfg = MkConfig(12,
          6, ChronoUnit.HOURS, 20, TimeUnit.MINUTES)
      cfg.pubSubUrl = "ws://127.0.0.1:8546"
      cfg.rootUrl = "http://127.0.0.1:8545"
      eth = ParityTransport(cfg, ethCache)
    }

    it("Opens a new websocket Parity connection and processes incoming messages") {
      Thread.sleep(90_000)
      eth?.close()
    }

    it("Can encode 0.05438066 ETH as base 16 wei.") {
      val ethAmt = BigDecimal("0.05438066").setScale(18)
      val wei16 = eth!!.encodeAmount(ethAmt)
      val expected = "0xC132EC11F38800".toLowerCase()
      assertEquals(expected, wei16)
    }
    it("Can update the ETH cache.") { eth!!.update() }
    it("Can skip ETH cache update if it's up to date.") { eth!!.update() }
    it("Can find transactions recorded in the last 4 hours.") {
      val utc4HrAgo = eth!!.nowUtcSecMinus(4, ChronoUnit.HOURS)
      val tx = ethCache.paymentById.values.filter { it.timeUtcSec >= utc4HrAgo }
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

    it("Creates a synthetic new block event to track specific transaction notifications.") {
      val tx40ByAddr = tx40Min.groupBy { it.address }
      val addr = tx40ByAddr.keys.iterator().next()
      val txList = tx40ByAddr[addr]
      val firstTx = txList!![0]
      val block = eth!!.getBlockDetail(firstTx.blockHeight)
      eth!!.notifyOnAddress(firstTx)
      val blockTx: MutableList<MkPaymentRecord> = ArrayList()
      eth!!.onAddressMatch = {
        log.info("New block transaction address filter match: [{}]", it)
        blockTx.add(it)
      }
      eth!!.newBlock(block)
      assert(blockTx.size <= txList.size)
    }

    it("Can get all transactions for a particular address.") {
      val addrTx = eth!!.getPaymentsFor(testAddress!!)
      assertTrue(addrTx.isNotEmpty())
    }
    it("Can compute a transaction fee.") {
      val tx = eth!!.getPaymentsFor(testAddress!!)[0]
      val gasPrice = BigInteger.valueOf(6_000_000_000L)
      val fee = eth!!.computeFee(MkPaymentDetail(MkAccount(), tx), listOf(), gasPrice)
      assertEquals(BigInteger.valueOf(126_000_000_000_000L), fee)
    }
    it("Can purge the cache for records older than 5 seconds.") { eth!!.purge() }
    it("Can create a new payment, along with a backing account.") {
      val payment = eth!!.create()
      assertNotNull(payment)
      assertNotNull(payment.gcmKey)
      val keyData = MkAccountCodec.decode(payment)
      assertNotNull(keyData)
      log.info(keyData)
    }
  }
}
