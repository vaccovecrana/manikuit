package io.vacco.mk.base

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import javax.validation.constraints.Size

data class MkAccount(
    @JsonProperty("crypto")
    @JsonPropertyDescription("A crypto currency type.")
    val crypto: MkExchangeRate.Crypto = MkExchangeRate.Crypto.UNKNOWN,

    @JsonProperty("address")
    @JsonPropertyDescription("A crypto currency address.")
    @Size(min = 32, max = 128)
    val address: String = "",

    @JsonProperty("cipherText")
    @JsonPropertyDescription("AES GCM implementation specific encoded address private components (key data/passphrase, etc.).")
    @Size(min = 16, max = 2048)
    val cipherText: String = "",

    @JsonProperty("iv")
    @Size(max = 256)
    val iv: String = "",

    @JsonProperty("gcmKey")
    @Size(max = 256)
    val gcmKey: String = ""
)
