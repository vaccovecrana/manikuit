package io.vacco.mk.base

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import io.vacco.metolithe.annotations.*
import javax.validation.constraints.*

@MtEntity
data class MkBlock(
    @MtId @NotNull
    @JsonPropertyDescription("Internal 64-bit hash/id (block no + block hash + block type).")
    var id: Long = -1,

    @MtIdGroup(number = 0, position = 0)
    @MtAttribute(nil = false)
    @DecimalMin("0")
    @JsonPropertyDescription("Block height.")
    var height: Long = 0,

    @MtAttribute(nil = false)
    @DecimalMin("0")
    @JsonPropertyDescription("Block time in UNIX epoch seconds.")
    var timeUtcSec: Long = 0,

    @MtIdGroup(number = 0, position = 1)
    @MtAttribute(nil = false, len = 128)
    @Size(min = 32, max = 128)
    @JsonPropertyDescription("Implementation specific block hash.")
    var hash: String = "",

    @MtIdGroup(number = 0, position = 2)
    @MtIndex @MtAttribute(nil = false, len = 32)
    @JsonPropertyDescription("A crypto currency type.")
    var type: MkExchangeRate.Crypto = MkExchangeRate.Crypto.UNKNOWN
)
