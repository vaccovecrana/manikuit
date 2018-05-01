package io.vacco.mk.base.eth

import javax.validation.constraints.NotNull
import com.fasterxml.jackson.annotation.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("blockHash", "blockNumber", "hash", "input", "transactionIndex",
    "from", "to", "gasPrice", "gas", "value", "nonce")
class Transaction(
    @JsonProperty("blockHash")
    val blockHash: String? = null,
    @JsonProperty("blockNumber")
    val blockNumber: String? = null,

    @JsonProperty("hash")
    val hash: String? = null,
    @JsonProperty("input")
    val input: String? = null,
    @JsonProperty("transactionIndex")
    val transactionIndex: String? = null,

    @NotNull @JsonProperty("from")
    val from: String = "",
    @NotNull @JsonProperty("to")
    val to: String? = null,

    @NotNull @JsonProperty("gasPrice")
    val gasPrice: String = "",
    @NotNull @JsonProperty("gas")
    val gas: String = "",

    @NotNull @JsonProperty("value")
    val value: String = "",
    @JsonProperty("nonce")
    val nonce: String? = null
)
