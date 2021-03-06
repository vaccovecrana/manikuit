package io.vacco.mk.config

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.validation.constraints.DecimalMin

/**
 * Base RPC source connection parameters.
 */
open class MkConfig(
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
    var blockCacheLimitUnit: TimeUnit = TimeUnit.NANOSECONDS,
    @JsonPropertyDescription("Pub/sub URI to receive implementation specific notifications from the processing daemon.")
    var pubSubUrl: String = "",
    @JsonPropertyDescription("Bloom filter address listener capacity (currently supports per-address listener filters).")
    @DecimalMin(value = "512")
    var addrFilterCapacity: Int = 10_000_000,
    @JsonPropertyDescription("Bloom filter address listener max false positive probability.")
    @DecimalMin(value = "0.0")
    var addrFilterMaxFalsePositiveProbability: Double = 0.001,
    @JsonPropertyDescription("Bloom filter transaction listener capacity (currently supports per-transaction listener filters).")
    @DecimalMin(value = "512")
    var txFilterCapacity: Int = 50_000_000,
    @JsonPropertyDescription("Bloom filter transaction listener max false positive probability.")
    @DecimalMin(value = "0.0")
    var txFilterMaxFalsePositiveProbability: Double = 0.001
): HttpConfig()