package unit

import io.vacco.mk.base.*
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
  private val btcScale = 8

  init {
    it("Cannot split funds if target percentages do not add up to one",
        {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(oneBtcInSatoshi.add(btcFee), btcFee, 8,
          MkSplit.FeeMode.PER_TRANSACTION,
          listOf(
              MkPaymentTarget("00", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("01", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("02", BigDecimal.valueOf(0.15)),
              MkPaymentTarget("03", BigDecimal.valueOf(0.70))
          )
      )
    })
    it("Cannot split funds without split targets", {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(oneBtcInSatoshi.add(btcFee), btcFee, btcScale, MkSplit.FeeMode.PER_TRANSACTION, listOf())
    })
    it("Cannot split funds with negative amounts", {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(BigInteger.ONE.negate(), btcFee, btcScale, MkSplit.FeeMode.PER_TRANSACTION, listOf())
    })
    it("Cannot split funds if the transaction fee is equal to the available funds amount",
        {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(oneBtcInSatoshi, oneBtcInSatoshi, btcScale, MkSplit.FeeMode.PER_TRANSACTION, listOf())
    })
    it("Cannot split funds if the transaction fee is higher to the available funds amount",
        {c -> c.expected(IllegalArgumentException::class.java)}, {
      MkSplit.apply(oneBtcInSatoshi, oneBtcInSatoshi.add(btcFee), btcScale, MkSplit.FeeMode.PER_TRANSACTION, listOf())
    })

    it("Splits 1BTC plus miner fee in 4 equal parts") {
      val splitResult = MkSplit.apply(oneBtcInSatoshi.add(btcFee), btcFee, btcScale,
          MkSplit.FeeMode.PER_TRANSACTION,
          listOf(
              MkPaymentTarget("00", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("01", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("02", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("03", BigDecimal.valueOf(0.25)))
      )
      val summedSplit = splitResult.map { tg -> tg.amount }.reduce { amt0, amt1 -> amt0.add(amt1) }
      assertEquals(oneBtcInSatoshi, summedSplit)
      log.info(splitResult.toString())
    }
    it("Splits 1BTC without miner fee in 4 equal parts") {
      val splitResult = MkSplit.apply(oneBtcInSatoshi, btcFee, btcScale,
          MkSplit.FeeMode.PER_TRANSACTION,
          listOf(
              MkPaymentTarget("00", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("01", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("02", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("03", BigDecimal.valueOf(0.25)))
      )
      val summedSplit = splitResult.map { tg -> tg.amount }.reduce { amt0, amt1 -> amt0.add(amt1) }
      assertEquals(BigInteger.valueOf(99_916_000), summedSplit)
      log.info(splitResult.toString())
    }
    it("Splits 1BTC with miner fee in uneven parts") {
      val splitResult = MkSplit.apply(oneBtcInSatoshi.add(btcFee), btcFee, btcScale,
          MkSplit.FeeMode.PER_TRANSACTION,
          listOf(
              MkPaymentTarget("00", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("01", BigDecimal.valueOf(0.25)),
              MkPaymentTarget("02", BigDecimal.valueOf(0.15)),
              MkPaymentTarget("03", BigDecimal.valueOf(0.10)),
              MkPaymentTarget("04", BigDecimal.valueOf(0.05)),
              MkPaymentTarget("05", BigDecimal.valueOf(0.05)),
              MkPaymentTarget("06", BigDecimal.valueOf(0.10)),
              MkPaymentTarget("07", BigDecimal.valueOf(0.05))
          )
      )
      val summedSplit = splitResult.map { tg -> tg.amount }.reduce { amt0, amt1 -> amt0.add(amt1) }
      assertEquals(oneBtcInSatoshi, summedSplit)
      log.info(splitResult.toString())
    }
  }
}