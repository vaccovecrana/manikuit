package io.vacco.mk.base.eth

import javax.validation.constraints.NotNull
import com.fasterxml.jackson.annotation.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("blockHash", "blockNumber", "from", "gas", "gasPrice", "" +
    "hash", "input", "nonce", "to", "transactionIndex", "value", "v", "r", "s")
class Transaction(
    @NotNull
    @JsonProperty("blockHash")
    val blockHash: String = "",
    @NotNull
    @JsonProperty("blockNumber")
    val blockNumber: String = "",
    @NotNull
    @JsonProperty("from")
    val from: String = "",
    @NotNull
    @JsonProperty("gas")
    val gas: String = "",
    @NotNull
    @JsonProperty("gasPrice")
    val gasPrice: String = "",
    @NotNull
    @JsonProperty("hash")
    val hash: String = "",
    @NotNull
    @JsonProperty("input")
    val input: String = "",
    @NotNull
    @JsonProperty("nonce")
    val nonce: String = "",
    @NotNull
    @JsonProperty("to")
    val to: String? = "",
    @NotNull
    @JsonProperty("transactionIndex")
    val transactionIndex: String = "",
    @NotNull
    @JsonProperty("value")
    val value: String = "",
    @JsonProperty("v")
    val v: String = "",
    @JsonProperty("r")
    val r: String = "",
    @JsonProperty("s")
    val s: String = ""
)
