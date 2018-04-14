package io.vacco.mk.base

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import io.vacco.metolithe.annotations.*
import javax.validation.constraints.*

@MtEntity
data class MkBlock(

    @MtId @MtAttribute(len = 32)
    @Size(min = 32, max = 32)
    @JsonPropertyDescription("Internal hash/id (block no + block hash + block type).")
    var id: String = "",

    @MtAttribute(nil = false)
    @DecimalMin("0")
    @JsonPropertyDescription("Block height.")
    var height: Long = 0,

    @MtAttribute(nil = false)
    @DecimalMin("0")
    @JsonPropertyDescription("Block time in UNIX epoch seconds.")
    var timeUtcSec: Long = 0,

    @MtAttribute(nil = false, len = 128)
    @Size(min = 32, max = 128)
    @JsonPropertyDescription("Implementation specific block hash.")
    var hash: String = "",

    @MtIndex @MtAttribute(nil = false)
    @JsonPropertyDescription("A crypto currency type.")
    var type: MkExchangeRate.Crypto = MkExchangeRate.Crypto.UNKNOWN
)
