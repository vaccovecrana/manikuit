package unit

import io.vacco.mk.base.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.rpc.BitcoindTransport
import io.vacco.mk.util.NoOpBlockCache
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.*
import org.slf4j.*
import org.junit.Assert.*
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

  // Update these as test net coins get transferred/depleted
  private var seedAccount = MkAccount(
      type = MkExchangeRate.Crypto.BTC,
      address = "misV7bW9EM67X3r8ENv7hYnsk2eppvQxf6",
      cipherText = "9Co61QQGLc6eYGkI+CVcB3B7wObPXlF6Pkw2mLPAk5LlTlAQ086z2IiY6zVJ1oyOTzfwM7/1Y3UqGCtlXmP4L7i6xrI=",
      iv = "xfj7HcNoKU45TvO3", gcmKey = "UpNq7gNLhPfqzT7e+S3axHTLYZnHoOo4fplf8dWBAIk="
  )
  private var seedPayment: MkPaymentRecord? = null

  private var targetAcct0: MkAccount? = null
  private var targetAcct1: MkAccount? = null
  private var targetAcct2: MkAccount? = null

  private val returnAddress0 = "2NBwr4VLa4fFu4cmztoXZWVzKV3gnmdoyc7"
  private val returnAddress1 = "2NArJYqWyFyFMjP3Ytx96HxBEN6fRwFnLKp"
  private val returnAddress2 = "2Myys3UNkxZz4Xvtm7DkU6WdAMmtCZD1uh3"

  init {
    beforeAll {
      val cfg = MkConfig(6, 1, ChronoUnit.HOURS, 10, TimeUnit.SECONDS)
      cfg.pubSubUrl = "tcp://127.0.0.1:28332"
      cfg.rootUrl = "http://127.0.0.1:18332"
      cfg.username = "gopher"
      cfg.password = "omglol"
      cfg.connectionPoolSize = 8
      btc = BitcoindTransport(cfg, NoOpBlockCache())
    }
    it("Seeds the BTC source account") {
      // macos command: "/bin/bash", "-c", "qrencode -o - bitcoin:${seedAccount.address}?amount=0.00500000 | open -f -a preview"
      ProcessBuilder("/bin/bash", "-c", "qrencode -o - bitcoin:${seedAccount.address}?amount=0.00500000 | display").start()
      log.info("Send 0.00500000 BTC from a funding account...")
      seedPayment = MkPaymentUtil.awaitPayment(btc!!, seedAccount.address)[seedAccount.address]
      MkPaymentUtil.awaitConfirmation(seedPayment!!, btc!!)
    }
    it("Splits the seed payment into 3 accounts and relays funds back to target return addresses") {
      assertNotNull(seedPayment)
      assertEquals(MkPaymentRecord.Status.COMPLETE, btc!!.getStatus(seedPayment!!, btc!!.getLatestBlockNumber()))
      targetAcct0 = btc!!.create()
      targetAcct1 = btc!!.create()
      targetAcct2 = btc!!.create()

      val acctIdx = mapOf(
          targetAcct0!!.address to targetAcct0,
          targetAcct1!!.address to targetAcct1,
          targetAcct2!!.address to targetAcct2
      )
      val txFee = BigInteger.valueOf(159_501)

      val statusMap = btc!!.broadcast(MkPaymentDetail(seedAccount, seedPayment!!), listOf(
          MkPaymentTarget(targetAcct0!!.address, 30),
          MkPaymentTarget(targetAcct1!!.address, 35),
          MkPaymentTarget(targetAcct2!!.address, 35)
      ), txFee, txFee)
      assertTrue(statusMap.isNotEmpty())

      val paymentsIn = MkPaymentUtil
          .awaitPayment(btc!!, *statusMap.map { it.address }.toTypedArray())
          .values.map { MkPaymentDetail(acctIdx[it.address]!!, it) }

      val paymentsOut = arrayOf(
          btc!!.broadcast(paymentsIn[0], listOf(MkPaymentTarget(returnAddress0, 100)), txFee, txFee).toTypedArray()[0],
          btc!!.broadcast(paymentsIn[1], listOf(MkPaymentTarget(returnAddress1, 100)), txFee, txFee).toTypedArray()[0],
          btc!!.broadcast(paymentsIn[2], listOf(MkPaymentTarget(returnAddress2, 100)), txFee, txFee).toTypedArray()[0]
      )

      MkPaymentUtil.awaitPayment(btc!!, *paymentsOut.map { it.address }.toTypedArray())
          .values.map { MkPaymentUtil.awaitConfirmation(it, btc!!) }
    }
  }
}
