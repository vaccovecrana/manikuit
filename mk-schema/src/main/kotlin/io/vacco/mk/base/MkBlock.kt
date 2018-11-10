package io.vacco.mk.base

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import io.vacco.metolithe.annotations.*
import javax.validation.constraints.*

@MtEntity
data class MkBlock(
    @MtId @NotNull
    @JsonPropertyDescription("Internal 64-bit hash/id (block no + block hash + block type).")
    var id: Long = -1,

    @MtId(position = 1)
    @MtAttribute(nil = false)
    @DecimalMin("0")
    @JsonPropertyDescription("Block height.")
    var height: Long = 0,

    @MtAttribute(nil = false)
    @DecimalMin("0")
    @JsonPropertyDescription("Block time in UNIX epoch seconds.")
    var timeUtcSec: Long = 0,

    @MtId(position = 2)
    @MtAttribute(nil = false, len = 128)
    @Size(min = 32, max = 128)
    @JsonPropertyDescription("Implementation specific block hash.")
    var hash: String = "",

    @MtId(position = 3)
    @MtIndex @MtAttribute(nil = false, len = 32)
    @JsonPropertyDescription("A crypto currency type.")
    var type: MkExchangeRate.Crypto = MkExchangeRate.Crypto.UNKNOWN
)
