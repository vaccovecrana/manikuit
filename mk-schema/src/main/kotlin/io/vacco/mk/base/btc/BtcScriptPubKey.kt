package io.vacco.mk.base.btc

import java.util.ArrayList
import javax.validation.Valid
import com.fasterxml.jackson.annotation.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("addresses", "asm", "hex", "reqSigs", "type")
data class BtcScriptPubKey(
    @JsonProperty("addresses") @Valid
    val addresses: List<String>? = ArrayList(),
    @JsonProperty("asm")
    val asm: String = "",
    @JsonProperty("hex")
    val hex: String = "",
    @JsonProperty("reqSigs")
    val reqSigs: Long = 0,
    @JsonProperty("type")
    val type: String = ""
)
