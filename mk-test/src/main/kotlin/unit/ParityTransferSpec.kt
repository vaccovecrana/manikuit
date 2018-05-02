package unit

import io.vacco.mk.base.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.rpc.ParityTransport
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.*
import org.junit.Assert.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import util.InMemoryBlockCache
import util.MkPaymentUtil
import java.math.BigInteger
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class ParityTransferSpec {

  private val log: Logger = LoggerFactory.getLogger(javaClass)

  private var eth: ParityTransport? = null

  // Update these as Kovan testnet ether gets transferred/depleted
  private val seedAccount = MkAccount(
      type = MkExchangeRate.Crypto.ETH,
      address = "0x61413e53f57fbee607333704e6b110654fcfae86",
      cipherText = "kF/rvbBpNaMhcqM+2Z/foAtz3TuUCUHBBBInobw74ySyY0nY0apwRBVgk+16NHKnzSirAHspQzxPY0ozEfr8TAp7ZCTQdzYKhBkKG0x+D0Q1XcU6FaVTMwoV2rove6eFXwX0jm+og9XuixA0oQmUAUp2aeMEyyj16vCp88azQUY6fGdxwO2F6OSPdfrJSVPhmLwlpUTqSdOKUPSPstTMowSj4QugucBMeOEk5Uj6wEnZkv21Nrh3rMqa0VTGKRvM+x0VRvjixj5Pwf7xoGHPY0xZsdVOwuaH+ITFo48HH0yPk5qpFWcyKT1HPM2ny+66rNv/IYb7yBGX14i59ZMzdI6USD2a6JocdJ11uVVx6ZoHRVy42Ivgmh/4CgpBgph+4Y5hjatvYhLTDn9UpFKmHZSeeklaK2yGaOS2qK5ofmEi1qechRbJ7ud48GS7oyTrEm3CZB2q0bByenGL8oOO3/QcIVpchv8cZDgFhEm2NhTQWoeEL5t8KAXKNH4xYDaN0UHVqhETjxOzdbpRbsxOraWVrLb1Qinrqx4sG4roXARvjnDpMin2+unfClnCr/oZTCpaIxc+jNkyoqClENx2FGTmva0aJqV4u+V2NmvsYySdWq/B8sT1mH4lbxh64UN5T5ZVrw+zjhsAxHUVGm1bipGNxXv5en9QoFITiWRGj37vhb7XYDAXk2/aquxyNNnTfyWRUoHv2RQiSd1rwDG5sbNeJVSYOmUK7DOiYB03sx5TJ2nOC7CiZH5oebewPA==",
      iv = "wlxna16Djvm9nwxb", gcmKey = "3GndrdKSZ6CxXjdfDwESm6Z756Z1jzKQMmlABv4+QJ4=")

  private val seedBlock = 6420381L
  private val seedTx = "0x848d6eb0c16a0937778fe9ee7f4a835158af0357e9b1deba0cc4d632759a1ae3"
  private val seedAmount = BigInteger.valueOf(14_000_000_000_000_000) // 0.014 ETH in wei
  private var seedPayment: MkPaymentRecord? = null

  private var targetAcct0: MkAccount? = null
  private var targetAcct1: MkAccount? = null
  private var targetAcct2: MkAccount? = null

  private val gasPrice = BigInteger.valueOf(6_000_000_000L)
  private val fee = BigInteger.valueOf(126_000_000_000_000L)

  init {
    beforeAll {
      val cfg = MkConfig(12, 1, ChronoUnit.HOURS, 2, TimeUnit.HOURS)
      cfg.pubSubUrl = "ws://127.0.0.1:8546"
      cfg.rootUrl = "http://127.0.0.1:8545"
      eth = ParityTransport(cfg, InMemoryBlockCache())
    }/*
    it("Seeds the ETH source account") {
      // ProcessBuilder("/bin/bash", "-c", "qrencode -o - ${seedAccount.address} | open -f -a preview").start()
      log.info("Send me coin! :D")
      val payment = MkPaymentUtil.awaitPayment(seedAccount.address, null, eth!!.encodeHexInt(seedAmount), eth!!)
      MkPaymentUtil.awaitConfirmation(payment, eth!!)
    }*/
    it("Splits the seed payment into 3 accounts") {
      seedPayment = eth!!.getBlockDetail(eth!!.getBlock(seedBlock)).second
          .filter { it.txId == seedTx }.first { it.amount == eth!!.encodeHexInt(seedAmount) }
      assertNotNull(seedPayment)
      assertEquals(MkPaymentRecord.Status.COMPLETE, eth!!.getStatus(seedPayment!!, eth!!.getLatestBlockNumber()))
      targetAcct0 = eth!!.create()
      targetAcct1 = eth!!.create()
      targetAcct2 = eth!!.create()
      val statusMap = eth!!.broadcast(MkPaymentDetail(seedAccount, seedPayment!!), listOf(
          MkPaymentTarget(targetAcct0!!.address, 25),
          MkPaymentTarget(targetAcct1!!.address, 40),
          MkPaymentTarget(targetAcct2!!.address, 35)), gasPrice, fee)
      eth!!.onNewBlock = {
        val ta0 = it.second.firstOrNull { it.address == targetAcct0!!.address }
        val ta1 = it.second.firstOrNull { it.address == targetAcct1!!.address }
        val ta2 = it.second.firstOrNull { it.address == targetAcct2!!.address }
        log.info("Block [${it.first.hash}]")
        log.info("ta0 -> $ta0")
        log.info("ta1 -> $ta1")
        log.info("ta2 -> $ta2")
      }
      assertTrue(statusMap.isNotEmpty())
      statusMap
          .map { MkPaymentUtil.awaitPayment(it.address, it.txId, null, eth!!) }
          .map { MkPaymentUtil.awaitConfirmation(it, eth!!) }
    }
  }
}
