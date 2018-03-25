package io.vacco.mk.base.btc

import com.fasterxml.jackson.annotation.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("asm", "hex")
data class BtcScriptSig(
    @JsonProperty("asm")
    val asm: String = "",
    @JsonProperty("hex")
    val hex: String = ""
)
