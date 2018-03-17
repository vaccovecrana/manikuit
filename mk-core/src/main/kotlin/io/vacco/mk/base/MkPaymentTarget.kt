package io.vacco.mk.base

import java.math.BigInteger

data class MkPaymentTarget(val address: String, val pctAmount: Long,
                           var amount: BigInteger = BigInteger.ONE) {
  fun validate() {
    requireNotNull(address)
    require(pctAmount > 0)
    requireNotNull(amount)
    require(amount.compareTo(BigInteger.ZERO) == 1)
  }
}
