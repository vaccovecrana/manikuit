package io.vacco.mk.base.btc

import java.util.ArrayList
import javax.validation.Valid
import com.fasterxml.jackson.annotation.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("coinbase", "scriptSig", "sequence", "txid", "txinwitness", "vout")
data class Vin(
    @JsonProperty("coinbase")
    @JsonPropertyDescription("The coinbase (similar to the hex field of a scriptSig) encoded as hex. Only present if this is a coinbase transaction.")
    val coinbase: String? = "",
    @JsonProperty("scriptSig") @Valid
    val scriptSig: BtcScriptSig? = null,
    @JsonProperty("sequence")
    val sequence: Long = 0,
    @JsonProperty("txid")
    val txid: String? = "",
    @JsonProperty("txinwitness")
    @JsonPropertyDescription("Added in Bitcoin Core 0.13.0 Hex-encoded witness data. Only for segregated witness transactions")
    @Valid
    val txinwitness: List<String>? = ArrayList(),
    @JsonProperty("vout")
    val vout: Long = 0
)
