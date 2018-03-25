package io.vacco.mk.base.rpc

import java.util.ArrayList
import javax.validation.Valid
import javax.validation.constraints.NotNull
import com.fasterxml.jackson.annotation.*

/**
 * Core JSON RPC request.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("jsonrpc", "id", "method", "params")
data class RpcRequest(
    @JsonProperty("jsonrpc")
    @JsonPropertyDescription("Version indicator for the JSON-RPC request.")
    val jsonrpc: String = "2.0",
    @JsonProperty("id")
    @JsonPropertyDescription("An arbitrary string that will be returned with the response. May be omitted or set to an empty string (\u201c\u201d)")
    val id: String = "",
    @NotNull
    @JsonProperty("method")
    @JsonPropertyDescription("The RPC method name (e.g. getblock). See the RPC section for a list of available methods.")
    val method: String = "",
    @JsonProperty("params")
    @Valid
    val params: List<Any> = ArrayList()
)
