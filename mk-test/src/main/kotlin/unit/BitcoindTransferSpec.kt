package unit

import io.vacco.mk.base.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.rpc.BitcoindTransport
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.*
import org.slf4j.*
import org.junit.Assert.*
import util.InMemoryBlockCache
import util.MkPaymentUtil
import java.math.BigInteger
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class BitcoindTransferSpec {

  private val log: Logger = LoggerFactory.getLogger(javaClass)
  private var btc: BitcoindTransport? = null

  /*
  MkAccount(type=btc, address=mow3ZN6g9QqVkVEQnnk8QFBhy9J5qPgZez, cipherText=U1gg/8CBQW6qoFGwkBgcXB0qV9jV3mnOVsNchhYGB79tHUU3uzv5mV9mA6dz0m//fbMpWFI4WvdiNxl31k6YrTsprXs=, iv=wqfNAu6gTbIDGJJA, gcmKey=dh4KMI5TcLGgPF9uq25WnOpDenm6JrAqJ8UjhG8iW8g=)
  MkAccount(type=btc, address=msGqgEUjoMdWVcPXQNHdi14LQqEPyvEmJx, cipherText=+tDvYmSUsdjQkPtt4DzfxaXw/XH00H3+BiynngiC24AtBGOCvs5Ec79IoiN11JyW4hbWc1SYwOKDgtiFL+uwBvOlA2k=, iv=8C5iRg0saTNXE5DQ, gcmKey=y06sW7mmubQBCVrelPXDEwAOJIuLEReg4qsEOFsERug=)
  MkAccount(type=btc, address=n3cPjudEELsUqTQCzzwSPyvMwk9JZuxBZx, cipherText=m0M5rgk2Kh7krN+H0ADJx3rihmLuws4SfBSdwhoNhEyHF/iWzPh7sw/uyo6LGdFZGw0jSpcKLQJbhpd4mq84cegAp1o=, iv=bQnikyrOtasIhGhG, gcmKey=O7XasO1hyT4YM3nKenR4MpELQZG5KhQ+4Zd+/aL/mgY=)
   */

  // Update these as testnet coins get transferred/depleted
  private var seedAccount = MkAccount(
      type=MkExchangeRate.Crypto.BTC,
      address="misV7bW9EM67X3r8ENv7hYnsk2eppvQxf6",
      cipherText="9Co61QQGLc6eYGkI+CVcB3B7wObPXlF6Pkw2mLPAk5LlTlAQ086z2IiY6zVJ1oyOTzfwM7/1Y3UqGCtlXmP4L7i6xrI=",
      iv="xfj7HcNoKU45TvO3", gcmKey="UpNq7gNLhPfqzT7e+S3axHTLYZnHoOo4fplf8dWBAIk="
  )
  private val seedBlock = 1288402L
  private val seedTx = "0ec55962e61a637714d8a1006d2c1392f4a71e1df0c8c39d11a51255072ce15a"
  private var seedPayment: MkPaymentRecord? = null
  private val seedAmount = "0.01800000" // BTC

  private var targetAcct0: MkAccount? = null
  private var targetAcct1: MkAccount? = null
  private var targetAcct2: MkAccount? = null

  init {
    beforeAll {
      val cfg = MkConfig(6, 1, ChronoUnit.HOURS, 10, TimeUnit.SECONDS)
      cfg.pubSubUrl = "tcp://127.0.0.1:28332"
      cfg.rootUrl = "http://127.0.0.1:18332"
      cfg.username = "gopher"
      cfg.password = "omglol"
      cfg.connectionPoolSize = 8
      btc = BitcoindTransport(cfg, InMemoryBlockCache())
      btc!!.update()
    }
    it("Seeds the BTC source account") {
      ProcessBuilder("/bin/bash", "-c", "qrencode -o - bitcoin:${seedAccount.address}?amount=$seedAmount | open -f -a preview").start()
      log.info("Send me coin! :D")
      val payment = MkPaymentUtil.awaitPayment(seedAccount.address, null, seedAmount, btc!!)
      MkPaymentUtil.awaitConfirmation(payment, btc!!)
    }
    it("Splits the seed payment into 3 accounts") {
      seedPayment = btc!!.getBlockDetail(btc!!.getBlock(seedBlock)).second
          .filter { it.txId == seedTx }
          .first { it.amount == seedAmount }
      assertNotNull(seedPayment)
      assertEquals(MkPaymentRecord.Status.COMPLETE, btc!!.getStatus(seedPayment!!, btc!!.getLatestBlockNumber()))
      targetAcct0 = btc!!.create()
      targetAcct1 = btc!!.create()
      targetAcct2 = btc!!.create()
      val statusMap = btc!!.broadcast(MkPaymentDetail(seedAccount, seedPayment!!), listOf(
          MkPaymentTarget(targetAcct0!!.address, 30),
          MkPaymentTarget(targetAcct1!!.address, 35),
          MkPaymentTarget(targetAcct2!!.address, 35)
      ), BigInteger.valueOf(159_501))
      assertTrue(statusMap.isNotEmpty())
      statusMap
          .map { MkPaymentUtil.awaitPayment(it.address, it.txId, null, btc!!) }
          .map { MkPaymentUtil.awaitConfirmation(it, btc!!) }
    }
  }
}
