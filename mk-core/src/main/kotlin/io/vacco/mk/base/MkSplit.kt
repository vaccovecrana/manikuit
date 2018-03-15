package io.vacco.mk.base

import java.math.BigDecimal
import java.math.BigInteger

object MkSplit {

  enum class FeeMode { PER_TRANSACTION, PER_TARGET }

  fun apply(amount: BigInteger, txFee: BigInteger, coinPrecision: Int, mode: FeeMode,
            targets: Collection<MkPaymentTarget>): Collection<MkPaymentTarget> {
    requireNotNull(amount)
    require(amount.compareTo(BigInteger.ZERO) == 1)
    requireNotNull(txFee)
    requireNotNull(targets)
    require(targets.isNotEmpty())

    val totalSplit = targets.map { pt -> pt.pctAmount }.reduce { pct0, pct1 -> pct0.add(pct1) }
    require(totalSplit.compareTo(BigDecimal.ONE) == 0) // TODO add error msg, split pct does not add up to one.

    val netAmount = when (mode) {
      FeeMode.PER_TRANSACTION -> amount.subtract(txFee)
      FeeMode.PER_TARGET -> amount.subtract(txFee.multiply(BigInteger.valueOf(targets.size.toLong())))
    }

    val netAmountCmp = netAmount.compareTo(BigInteger.ZERO)
    require(netAmountCmp == 1)
    val netAmountBd = netAmount.toBigDecimal().setScale(coinPrecision)

    return targets.map { tg ->
      val targetAmount = tg.pctAmount.setScale(coinPrecision).multiply(netAmountBd)
      tg.copy(amount = targetAmount.toBigInteger())
    }
  }

}