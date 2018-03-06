package io.vacco.mk.base

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.onyx.persistence.ManagedEntity
import com.onyx.persistence.annotations.*
import javax.validation.constraints.*

@Entity
data class MkBlock(
    @Identifier
    @Attribute(nullable = false)
    @Size(min = 32, max = 128)
    @JsonPropertyDescription("Internal hash/id (block no + block hash + block type).")
    var id: String = "",

    @Attribute
    @Index(loadFactor = 8)
    @DecimalMin("0")
    @JsonPropertyDescription("Block height.")
    var height: Long = 0,

    @Attribute
    @DecimalMin("0")
    @JsonPropertyDescription("Block time in UNIX epoch seconds.")
    var timeUtcSec: Long = 0,

    @Attribute(nullable = false)
    @Size(min = 32, max = 128)
    @JsonPropertyDescription("Implementation specific block hash.")
    var hash: String = "",

    @Attribute(nullable = false)
    @Index(loadFactor = 8)
    @JsonPropertyDescription("A crypto currency type.")
    var type: MkAccount.Crypto = MkAccount.Crypto.UNKNOWN
) : ManagedEntity()
