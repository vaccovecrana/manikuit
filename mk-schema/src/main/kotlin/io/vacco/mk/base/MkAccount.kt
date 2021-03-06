package io.vacco.mk.base

import com.fasterxml.jackson.annotation.*
import io.vacco.metolithe.annotations.*
import javax.validation.constraints.*

@MtEntity
open class MkAccount(

    @MtId var aid: Long = -1,

    @MtIdGroup(number = 0, position = 0)
    @MtAttribute(nil = false, len = 32)
    @JsonProperty("type")
    @JsonPropertyDescription("A crypto currency type.") @NotNull
    var type: MkExchangeRate.Crypto = MkExchangeRate.Crypto.UNKNOWN,

    @MtIndex
    @MtIdGroup(number = 0, position = 1)
    @MtAttribute(nil = false, len = 128)
    @JsonProperty("address")
    @JsonPropertyDescription("A type currency address.")
    @Size(min = 32, max = 128) @NotNull
    var address: String = "",

    @MtAttribute(nil = false, len = 2048)
    @JsonProperty("cipherText")
    @JsonPropertyDescription("AES GCM implementation specific encoded address private components (key data/passphrase, etc.).")
    @Size(min = 16, max = 2048)
    var cipherText: String = "",

    @MtAttribute(nil = false, len = 256)
    @JsonProperty("iv")
    @Size(max = 256)
    var iv: String = "",

    @MtAttribute(nil = false, len = 256)
    @JsonProperty("gcmKey")
    @Size(max = 256)
    var gcmKey: String = ""
) {
    override fun toString(): String = "(${this.type}) ${this.address}"
}
