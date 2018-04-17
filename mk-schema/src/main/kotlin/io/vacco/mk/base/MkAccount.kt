package io.vacco.mk.base

import com.fasterxml.jackson.annotation.*
import io.vacco.metolithe.annotations.*
import javax.validation.constraints.*

@MtEntity
open class MkAccount(
    @MtAttribute(nil = false, len = 32)
    @JsonProperty("type")
    @JsonPropertyDescription("A crypto currency type.") @NotNull
    val type: MkExchangeRate.Crypto = MkExchangeRate.Crypto.UNKNOWN,

    @MtId @MtAttribute(nil = false, len = 128)
    @JsonProperty("address")
    @JsonPropertyDescription("A type currency address.")
    @Size(min = 32, max = 128) @NotNull
    val address: String = "",

    @MtAttribute(nil = false, len = 2048)
    @JsonProperty("cipherText")
    @JsonPropertyDescription("AES GCM implementation specific encoded address private components (key data/passphrase, etc.).")
    @Size(min = 16, max = 2048)
    val cipherText: String = "",

    @MtAttribute(nil = false, len = 256)
    @JsonProperty("iv")
    @Size(max = 256)
    val iv: String = "",

    @MtAttribute(nil = false, len = 256)
    @JsonProperty("gcmKey")
    @Size(max = 256)
    val gcmKey: String = ""
)
