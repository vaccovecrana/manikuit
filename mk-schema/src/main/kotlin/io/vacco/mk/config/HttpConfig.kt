package io.vacco.mk.config

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.util.concurrent.TimeUnit
import javax.validation.constraints.Size

/**
 * HTTP(S) base configuration.
 */
open class HttpConfig(
    @Size(min = 16)
    @JsonPropertyDescription("Base endpoint URL.")
    var rootUrl: String = "",
    @JsonPropertyDescription("RPC endpoint username.")
    var username: String = "",
    @JsonPropertyDescription("RPC endpoint password.")
    var password: String = "",
    @JsonPropertyDescription("Whether to ignore SSL peer validation.")
    var ignoreSsl: Boolean = false,
    @JsonPropertyDescription("Connection pool size.")
    var connectionPoolSize: Int = -1,
    @JsonPropertyDescription("Connection pool keep alive time.")
    var keepAlive: Long = 5,
    @JsonPropertyDescription("Connection pool keep alive time unit.")
    var keepAliveUnit: TimeUnit = TimeUnit.MINUTES
)