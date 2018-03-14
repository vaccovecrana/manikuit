package io.vacco.mk.base

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.onyx.persistence.ManagedEntity
import com.onyx.persistence.annotations.*
import javax.validation.constraints.*

@Entity
data class MkPaymentRecord(

    @Identifier
    @Attribute(nullable = false)
    @JsonPropertyDescription("Internal record hash.")
    var id: String = "",

    @Attribute(nullable = false)
    @JsonPropertyDescription("A crypto currency type.")
    var type: MkExchangeRate.Crypto = MkExchangeRate.Crypto.UNKNOWN,

    @Index(loadFactor = 8)
    @Attribute(nullable = false)
    @Size(min = 32, max = 128)
    @JsonPropertyDescription("A crypto currency address.")
    var address: String = "",

    @Attribute(nullable = false)
    @Size(min = 24, max = 128)
    @JsonPropertyDescription("A transaction hash.")
    var txId: String = "",

    @Attribute(nullable = false)
    @JsonPropertyDescription("Implementation specific string encoding for crypto currency payment amounts.")
    var amount: String = "",

    @Attribute(nullable = false)
    @DecimalMin("0")
    @JsonPropertyDescription("The first block height where this transaction was found.")
    var blockHeight: Long = 0,

    @Attribute(nullable = false)
    @DecimalMin("0")
    @JsonPropertyDescription("An implementation specific transaction output index (mostly 0 for non-utxo models).")
    var outputIdx: Long = 0,

    @Attribute(nullable = false)
    @DecimalMin("0")
    @JsonPropertyDescription("The UTC epoch timestamp of this transaction.")
    var timeUtcSec: Long = 0
) : ManagedEntity() { enum class Status { PENDING, COMPLETE } }
