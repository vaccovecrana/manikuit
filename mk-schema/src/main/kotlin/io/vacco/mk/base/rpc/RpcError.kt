package io.vacco.mk.base.rpc

import com.fasterxml.jackson.annotation.*

/**
 * An object describing the error if one occurred, otherwise `null`.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("code", "message")
class RpcError(
    @JsonProperty("code")
    @JsonPropertyDescription("The error code returned by the RPC function call. See rpcprotocol.h for a full list of error codes and their meanings.")
    var code: Long = 0,
    @JsonProperty("message")
    @JsonPropertyDescription("A text description of the error. May be an empty string (\u201c\u201d).")
    var message: String = ""
)
