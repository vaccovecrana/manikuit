package io.vacco.mk.util

import io.vacco.mk.base.MkPaymentTarget
import java.math.BigInteger

object MkSplit {

  private val oneHundred: BigInteger = BigInteger.valueOf(100)
  enum class FeeMode { PER_TRANSACTION, PER_TARGET }

  fun apply(amount: BigInteger, txFee: BigInteger, mode: FeeMode,
            targets: Collection<MkPaymentTarget>): Collection<MkPaymentTarget> {
    requireNotNull(amount)
    require(amount.compareTo(BigInteger.ZERO) == 1)
    requireNotNull(txFee)
    requireNotNull(targets)
    require(targets.isNotEmpty())
    targets.forEach { it.validate() }

    val totalSplit = targets.map { pt -> pt.pctAmount }.reduce { pct0, pct1 -> pct0 + pct1 }
    require(totalSplit == 100L)

    val netAmount = when (mode) {
      FeeMode.PER_TRANSACTION -> amount.subtract(txFee)
      FeeMode.PER_TARGET -> amount.subtract(txFee.multiply(BigInteger.valueOf(targets.size.toLong())))
    }

    val netAmountCmp = netAmount.compareTo(BigInteger.ZERO)
    require(netAmountCmp == 1)

    val assignments = targets.map { tg -> tg.copy(amount = netAmount.multiply(BigInteger.valueOf(tg.pctAmount)).divide(oneHundred)) }
    val splitSum = assignments.map { it.amount }.reduce { amt0, amt1 -> amt0 + amt1 }
    val difference = netAmount!!.subtract(splitSum)
    if (difference.compareTo(BigInteger.ZERO) == 1) {
      (0 until difference.toLong()).map {
        val targetIdx = it % targets.size
        val target = assignments[targetIdx.toInt()]
        target.amount = target.amount.add(BigInteger.ONE)
      }
    }
    return assignments
  }

}