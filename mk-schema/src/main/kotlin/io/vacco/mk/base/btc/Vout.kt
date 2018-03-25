package io.vacco.mk.base.btc

import javax.validation.Valid
import com.fasterxml.jackson.annotation.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("n", "scriptPubKey", "value")
data class Vout(
    @JsonProperty("n")
    val n: Long = 0,
    @JsonProperty("scriptPubKey")
    @Valid
    val scriptPubKey: BtcScriptPubKey = BtcScriptPubKey(),
    @JsonProperty("value")
    @JsonPropertyDescription("Number of satoshis to spend (encoded as decimal BTC :/). May be zero; the sum of all outputs may not exceed the sum of satoshis previously spent to the outpoints provided in the input section. ")
    val value: Double = 0.toDouble()
)
