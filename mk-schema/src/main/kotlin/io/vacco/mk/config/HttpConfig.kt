package io.vacco.mk.config

import com.fasterxml.jackson.annotation.JsonPropertyDescription
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
    var ignoreSsl: Boolean = false
)