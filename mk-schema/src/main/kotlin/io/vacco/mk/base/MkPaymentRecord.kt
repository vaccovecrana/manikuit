package io.vacco.mk.base

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import io.vacco.metolithe.annotations.*
import javax.validation.constraints.*

@MtEntity
open class MkPaymentRecord(
    @MtId
    @JsonPropertyDescription("Internal 64-bit record hash.")
    var id: Long = -1,

    @MtId(position = 1)
    @MtAttribute(nil = false, len = 32) @get:NotNull @set:NotNull
    @JsonPropertyDescription("A crypto currency type.")
    var type: MkExchangeRate.Crypto = MkExchangeRate.Crypto.UNKNOWN,

    @MtId(position = 2)
    @MtIndex @MtAttribute(nil = false, len = 128)
    @Size(min = 32, max = 128) @get:NotNull @set:NotNull
    @JsonPropertyDescription("A crypto currency address.")
    var address: String = "",

    @MtId(position = 3)
    @MtAttribute(nil = false, len = 64) @get:NotNull @set:NotNull
    @JsonPropertyDescription("Implementation specific string encoding for crypto currency payment amounts.")
    var amount: String = "",

    @MtId(position = 4)
    @MtAttribute(nil = false)
    @DecimalMin("0") @get:NotNull @set:NotNull
    @JsonPropertyDescription("The first block height where this transaction was found.")
    var blockHeight: Long = 0,

    @MtId(position = 5)
    @MtAttribute(nil = false, len = 128)
    @Size(min = 24, max = 128) @get:NotNull @set:NotNull
    @JsonPropertyDescription("A transaction hash.")
    var txId: String = "",

    @MtAttribute(nil = false)
    @DecimalMin("0") @get:NotNull @set:NotNull
    @JsonPropertyDescription("An implementation specific transaction output index (mostly 0 for non-utxo models).")
    var outputIdx: Long = 0,

    @MtAttribute(nil = false)
    @DecimalMin("0") @get:NotNull @set:NotNull
    @JsonPropertyDescription("The UTC epoch timestamp of this transaction.")
    var timeUtcSec: Long = 0

) {
    enum class Status { PENDING, PROCESSING, COMPLETE }
    override fun toString(): String =
        "(${this.type}) [${this.address}, ${this.amount}, ${this.blockHeight}]"
}
