package io.vacco.mk.base.btc

import javax.validation.Valid
import javax.validation.constraints.NotNull
import com.fasterxml.jackson.annotation.*
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("bits", "chainwork", "confirmations", "difficulty", "hash",
    "height", "mediantime", "merkleroot", "nonce", "previousblockhash", "nextblockhash",
    "size", "strippedsize", "time", "tx", "version", "versionHex", "weight")
data class BtcBlock(

    @JsonProperty("bits")
    val bits: String = "",

    @NotNull
    @JsonProperty("chainwork")
    @JsonPropertyDescription("(exactly 1) The estimated number of block header hashes miners had to check from the genesis block to this block, encoded as big-endian hex.")
    val chainwork: String? = null,

    @JsonProperty("confirmations")
    val confirmations: Long = 0,
    @JsonProperty("difficulty")
    val difficulty: Double = 0.toDouble(),
    @JsonProperty("hash")
    val hash: String = "",
    @JsonProperty("height")
    val height: Long = 0,
    @JsonProperty("mediantime")
    val mediantime: Long = 0,
    @JsonProperty("merkleroot")
    val merkleroot: String = "",
    @JsonProperty("nonce")
    val nonce: Long = 0,

    @JsonProperty("previousblockhash")
    @JsonPropertyDescription("The hash of the header of the previous block, encoded as hex in RPC byte order. Not returned for genesis block.")
    val previousblockhash: String? = null,
    @JsonProperty("nextblockhash")
    @JsonPropertyDescription("The hash of the next block on the best block chain, if known, encoded as hex in RPC byte order.")
    val nextblockhash: String? = null,

    @JsonProperty("size")
    val size: Long = 0,
    @JsonProperty("strippedsize")
    val strippedsize: Long = 0,
    @JsonProperty("time")
    val time: Long = 0,

    @JsonProperty("tx") @Valid
    val tx: MutableList<BtcTx> = ArrayList(),
    @JsonProperty("version")
    val version: Long = 0,
    @JsonProperty("versionHex")
    val versionHex: String = "",
    @JsonProperty("weight")
    val weight: Long = 0
) {
    override fun toString(): String =
        "BtcBlock[$height, $hash, $size, $time]"
}
