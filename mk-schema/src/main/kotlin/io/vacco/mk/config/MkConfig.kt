package io.vacco.mk.config

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.validation.constraints.DecimalMin

/**
 * Base RPC source connection parameters.
 */
data class MkConfig(
    @JsonPropertyDescription("Number of required confirmations to consider a transaction as final.")
    @DecimalMin("0")
    var confirmationThreshold: Long = 0,
    @JsonPropertyDescription("Time limit amount to scan blocks for address/transaction metadata.")
    @DecimalMin("0")
    var blockScanLimit: Long = 0,
    @JsonPropertyDescription("Time limit unit type for address/transaction metadata block scanning.")
    var blockScanLimitUnit: ChronoUnit = ChronoUnit.FOREVER,
    @JsonPropertyDescription("The time limit amount to keep cached copies of blocks and transactions.")
    @DecimalMin("0")
    var blockCacheLimit: Long = 0,
    @JsonPropertyDescription("Time limit unit type subset equivalent to {@link java.util.concurrent.TimeUnit} for keeping cached copies of blocks/transactions.")
    var blockCacheLimitUnit: TimeUnit = TimeUnit.NANOSECONDS
): HttpConfig()