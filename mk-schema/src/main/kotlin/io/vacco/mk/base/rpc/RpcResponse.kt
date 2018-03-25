package io.vacco.mk.base.rpc

import javax.validation.Valid
import com.fasterxml.jackson.annotation.*

/**
 * Core JSON RPC response structure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("jsonrpc", "id", "result", "error")
data class RpcResponse(
    @JsonProperty("jsonrpc")
    val jsonrpc: Double = 0.toDouble(),
    @JsonProperty("id")
    @JsonPropertyDescription("The value of id provided with the request. Has value {@code null} if the id field was omitted in the request.")
    val id: String = "",
    @JsonProperty("result")
    @JsonPropertyDescription("The RPC output whose type varies by call. Has value {@code null} if an error occurred.")
    val result: Any? = null,
    @JsonProperty("error")
    @JsonPropertyDescription("An object describing the error if one occurred, otherwise {@code null}.")
    @Valid
    val error: RpcError? = null
)
