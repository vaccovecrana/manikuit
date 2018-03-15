package io.vacco.mk.base

import java.math.BigDecimal
import java.math.BigInteger

data class MkPaymentTarget(val address: String,
                           val pctAmount: BigDecimal,
                           val amount: BigInteger = BigInteger.ZERO)
