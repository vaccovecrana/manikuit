package io.vacco.mk.base

import javax.validation.constraints.*
import com.fasterxml.jackson.annotation.*

/**
 * An exchange rate between a crypto currency and a fiat currency.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("crypto", "fiat", "last")
data class MkExchangeRate(
    @JsonProperty("crypto")
    @JsonPropertyDescription("A crypto currency type.")
    val crypto: MkExchangeRate.Crypto = Crypto.UNKNOWN,
    @JsonProperty("fiat")
    @Size(min = 3)
    val fiat: String = "",
    @JsonProperty("last") @NotNull
    val last: Double = 0.0
) { enum class Crypto { BTC, LTC, ETH, UNKNOWN } }
