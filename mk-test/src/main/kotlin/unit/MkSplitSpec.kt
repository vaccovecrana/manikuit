package unit

import io.vacco.mk.base.*
import io.vacco.mk.util.MkSplit
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import j8spec.J8Spec.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.slf4j.*
import java.math.*

@DefinedOrder
@RunWith(J8SpecRunner::class)
class MkSplitSpec {

  private val log: Logger = LoggerFactory.getLogger(javaClass)

  private val oneBtcInSatoshi = BigInteger.valueOf(100_000_000)
  private val btcFee = BigInteger.valueOf(84_000)

  private val oneEthInWei = BigInteger("1000000000000000000")
  private val ethFee = BigInteger("314159265350000")

  init {
    it("Cannot split funds if target percentages do not add up to one",
        {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(oneBtcInSatoshi.add(btcFee), btcFee,
          MkSplit.FeeMode.PER_TRANSACTION,
          listOf(
              MkPaymentTarget("00", 25),
              MkPaymentTarget("01", 25),
              MkPaymentTarget("02", 15),
              MkPaymentTarget("03", 70)
          )
      )
    })
    it("Cannot split funds without split targets", {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(oneBtcInSatoshi.add(btcFee), btcFee,  MkSplit.FeeMode.PER_TRANSACTION, listOf())
    })
    it("Cannot split funds with negative amounts", {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(BigInteger.ONE.negate(), btcFee,  MkSplit.FeeMode.PER_TRANSACTION, listOf())
    })
    it("Cannot split funds if the transaction fee is equal to the available funds amount",
        {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(oneBtcInSatoshi, oneBtcInSatoshi,  MkSplit.FeeMode.PER_TRANSACTION, listOf())
    })
    it("Cannot split funds if the transaction fee is higher to the available funds amount",
        {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(oneBtcInSatoshi, oneBtcInSatoshi.add(btcFee),  MkSplit.FeeMode.PER_TRANSACTION, listOf())
    })
    it("Splits 1BTC plus miner fee in odd parts") {
      val targets = listOf(
          MkPaymentTarget("00", 25),
          MkPaymentTarget("01", 25),
          MkPaymentTarget("02", 25),
          MkPaymentTarget("03", 25))
      val splitResult = MkSplit.apply(oneBtcInSatoshi.add(btcFee), btcFee, 
          MkSplit.FeeMode.PER_TRANSACTION, targets)
      val summedSplit = splitResult.map { tg -> tg.amount }.reduce { amt0, amt1 -> amt0.add(amt1) }
      assertEquals(oneBtcInSatoshi, summedSplit)
      log.info(splitResult.toString())
    }
    it("Splits 1BTC plus miner fee in even parts") {
      val splitResult = MkSplit.apply(oneBtcInSatoshi.add(btcFee), btcFee, 
          MkSplit.FeeMode.PER_TRANSACTION,
          listOf(
              MkPaymentTarget("00", 35),
              MkPaymentTarget("01", 35),
              MkPaymentTarget("02", 30))
      )
      val summedSplit = splitResult.map { tg -> tg.amount }.reduce { amt0, amt1 -> amt0.add(amt1) }
      assertEquals(oneBtcInSatoshi, summedSplit)
      log.info(splitResult.toString())
    }
    it("Splits 1BTC without miner fee in 4 equal parts") {
      val splitResult = MkSplit.apply(oneBtcInSatoshi, btcFee, 
          MkSplit.FeeMode.PER_TRANSACTION,
          listOf(
              MkPaymentTarget("00", 25),
              MkPaymentTarget("01", 25),
              MkPaymentTarget("02", 25),
              MkPaymentTarget("03", 25))
      )
      val summedSplit = splitResult.map { tg -> tg.amount }.reduce { amt0, amt1 -> amt0.add(amt1) }
      assertEquals(BigInteger.valueOf(99_916_000), summedSplit)
      log.info(splitResult.toString())
    }
    it("Splits 1BTC with miner fee in uneven parts") {
      val splitResult = MkSplit.apply(oneBtcInSatoshi.add(btcFee), btcFee, 
          MkSplit.FeeMode.PER_TRANSACTION,
          listOf(
              MkPaymentTarget("00", 25),
              MkPaymentTarget("01", 25),
              MkPaymentTarget("02", 15),
              MkPaymentTarget("03", 10),
              MkPaymentTarget("04", 5),
              MkPaymentTarget("05", 5),
              MkPaymentTarget("06", 10),
              MkPaymentTarget("07", 5)
          )
      )
      val summedSplit = splitResult.map { tg -> tg.amount }.reduce { amt0, amt1 -> amt0.add(amt1) }
      assertEquals(oneBtcInSatoshi, summedSplit)
      log.info(splitResult.toString())
    }
    it("Splits 0.01800000 BTC, plus a miner's fee in three parts") {
      val amount = BigInteger.valueOf(1_800_000)
      val fee = BigInteger.valueOf(159_501L)
      val targets = listOf(
          MkPaymentTarget("00", 30),
          MkPaymentTarget("01", 35),
          MkPaymentTarget("02", 35)
      )
      val splitResult = MkSplit.apply(amount, fee,  MkSplit.FeeMode.PER_TRANSACTION, targets)
      val summedSplit = splitResult.map { tg -> tg.amount }.reduce { amt0, amt1 -> amt0.add(amt1) }
      assertEquals(amount.subtract(fee), summedSplit)
      assertEquals(amount, summedSplit.add(fee))
      log.info(splitResult.toString())
    }

    it("Splits 1ETH in 4 equal parts, applying fees per target") {
      val splitResult = MkSplit.apply(oneEthInWei, ethFee,
          MkSplit.FeeMode.PER_TARGET,
          listOf(
              MkPaymentTarget("00", 25),
              MkPaymentTarget("01", 25),
              MkPaymentTarget("02", 25),
              MkPaymentTarget("03", 25))
      )
      val summedSplit = splitResult.map { tg -> tg.amount }.reduce { amt0, amt1 -> amt0.add(amt1) }
      val sumPlusFees = summedSplit.add(ethFee.multiply(BigInteger.valueOf(4)))
      assertEquals(oneEthInWei, sumPlusFees)
      log.info(splitResult.toString())
    }
  }
}