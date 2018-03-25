package io.vacco.mk.base.eth

import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotNull
import com.fasterxml.jackson.annotation.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("difficulty", "extraData", "gasLimit", "gasUsed", "hash",
    "logsBloom", "miner", "mixHash", "nonce", "number", "parentHash", "receiptsRoot",
    "sha3Uncles", "size", "stateRoot", "timestamp", "totalDifficulty", "transactions",
    "transactionsRoot", "uncles")
data class EthBlock(
    @JsonProperty("difficulty")
    var difficulty: String = "",
    @JsonProperty("extraData")
    var extraData: String = "",
    @NotNull
    @JsonProperty("gasLimit")
    var gasLimit: String = "",
    @NotNull
    @JsonProperty("gasUsed")
    var gasUsed: String = "",
    @NotNull
    @JsonProperty("hash")
    var hash: String = "",
    @JsonProperty("logsBloom")
    var logsBloom: String = "",
    @JsonProperty("miner")
    var miner: String = "",
    @JsonProperty("mixHash")
    var mixHash: String? = "",
    @NotNull
    @JsonProperty("nonce")
    var nonce: String? = "",
    @NotNull
    @JsonProperty("number")
    var number: String = "",
    @JsonProperty("parentHash")
    var parentHash: String = "",
    @JsonProperty("receiptsRoot")
    var receiptsRoot: String = "",
    @JsonProperty("sha3Uncles")
    var sha3Uncles: String = "",
    @JsonProperty("size")
    var size: String = "",
    @JsonProperty("stateRoot")
    var stateRoot: String = "",
    @NotNull
    @JsonProperty("timestamp")
    var timestamp: String = "",
    @JsonProperty("totalDifficulty")
    var totalDifficulty: String = "",
    @JsonProperty("transactions")
    @Valid
    var transactions: List<String> = ArrayList(),
    @JsonProperty("transactionsRoot")
    var transactionsRoot: String = "",
    @JsonProperty("uncles")
    @Valid
    var uncles: List<String> = ArrayList()
)
