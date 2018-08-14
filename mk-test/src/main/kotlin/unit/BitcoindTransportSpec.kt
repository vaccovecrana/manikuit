package unit

import com.fasterxml.jackson.databind.ObjectMapper
import io.vacco.mk.base.MkAccount
import io.vacco.mk.base.MkPaymentDetail
import io.vacco.mk.base.MkPaymentRecord
import io.vacco.mk.base.MkPaymentTarget
import io.vacco.mk.base.btc.BtcBlock
import io.vacco.mk.rpc.BitcoindTransport
import j8spec.J8Spec.*
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import java.time.temporal.ChronoUnit
import io.vacco.mk.config.MkConfig
import io.vacco.mk.rpc.MkAccountCodec
import org.junit.Assert.*
import org.slf4j.*
import util.InMemoryBlockCache
import java.math.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class BitcoindTransportSpec {

  private val log: Logger = LoggerFactory.getLogger(this.javaClass)

  private var btc: BitcoindTransport? = null
  private var btcCache: InMemoryBlockCache = InMemoryBlockCache()
  private val tx40Min: MutableList<MkPaymentRecord> = ArrayList()
  private val tx40MinWithStatus:MutableList<Pair<MkPaymentRecord, MkPaymentRecord.Status>> = ArrayList()
  private var testAddress: String? = null

  init {
    beforeAll {
      val cfg = MkConfig(6, 2, ChronoUnit.HOURS, 10, TimeUnit.MINUTES)
      cfg.pubSubUrl = "tcp://127.0.0.1:28332"
      cfg.rootUrl = "http://127.0.0.1:18332"
      cfg.username = "gopher"
      cfg.password = "omglol"
      cfg.connectionPoolSize = 8
      btc = BitcoindTransport(cfg, btcCache)
    }

    it("Can convert 84000 satoshis to BTC") {
      val btcVal = btc!!.toBtc(BigInteger.valueOf(84_000))
      val expectedVal = BigDecimal("0.00084")
      assertEquals(expectedVal, btcVal)
    }
    it("Can encode 27 USD as 0.0033 BTC (decimal satoshis)") {
      val usdInBtc = BigDecimal("0.0033")
      val btcSato = btc!!.encodeAmount(usdInBtc)
      assertEquals("0.00330000", btcSato)
    }
    it("Can parse a BTC main net json block") {
      val myString = String(Files.readAllBytes(Paths.get(
          javaClass.classLoader.getResource("btcblock.json")!!.toURI())))
      val block = ObjectMapper().readValue(myString, BtcBlock::class.java)
      log.info(block.toString())
    }
    it("Can create a new payment, along with a backing account.") {
      val payment = btc!!.create()
      assertNotNull(payment)
      assertNotNull(payment.cipherText)
      assertNotNull(payment.iv)
      assertNotNull(payment.gcmKey)
      val keyData = MkAccountCodec.decode(payment)
      assertNotNull(keyData)
      log.info(keyData)
    }
    it("Can update the BTC cache.") { btc!!.update() }
    it("Can skip a cache update if the local block cache is up to date.") { btc!!.update() }

    it("Can find transactions recorded in the last 40 minutes.") {
      val utc40MinAgo = btc!!.nowUtcSecMinus(40, ChronoUnit.MINUTES)
      val tx = btcCache.paymentById.values.filter { it.timeUtcSec >= utc40MinAgo }
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
    it("Can compute a transaction fee.") {
      val feeSatosPerByte = BigInteger.valueOf(64)
      val tx = btc!!.getPaymentsFor(testAddress!!)[0]
      val detail = MkPaymentDetail(MkAccount(), tx)
      val targets = listOf(
          MkPaymentTarget("mv5pkqqJ3hZwn3oNtaT39HU62w9jPpBGsu", 25),
          MkPaymentTarget("mzYaFx63twKRRP2NARPYbp6tE3BRgny2gY", 25),
          MkPaymentTarget("mxvUDjNvkLoSsdN1xrxjYdMMgcdE9XQtao", 25),
          MkPaymentTarget("mrWM47xpeF6rEP7XCG5GYVUmtpcPT5LnaU", 25)
      )
      val fee = btc!!.computeFee(detail, targets, feeSatosPerByte)
      assertTrue(fee.compareTo(BigInteger.ZERO) == 1)
    }
    it("Can purge the cache.") { btc!!.purge() }
    it("Opens an IPC socket, listens and forwards messages.") {
      Thread.sleep(60_000)
      btc?.close()
    }
  }
}