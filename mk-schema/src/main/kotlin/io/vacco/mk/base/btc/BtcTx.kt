package io.vacco.mk.base.btc

import java.util.ArrayList
import javax.validation.Valid
import com.fasterxml.jackson.annotation.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("blockhash", "blocktime", "confirmations", "hash", "hex",
    "locktime", "size", "time", "txid", "version", "vin", "vout", "vsize")
data class BtcTx(
    @JsonProperty("blockhash")
    val blockhash: String? = "",
    @JsonProperty("blocktime")
    val blocktime: Long? = 0,
    @JsonProperty("confirmations")
    val confirmations: Long? = 0,
    @JsonProperty("hash")
    val hash: String = "",
    @JsonProperty("hex")
    val hex: String? = "",
    @JsonProperty("locktime")
    val locktime: Long? = 0,
    @JsonProperty("size")
    val size: Long = 0,
    @JsonProperty("time")
    val time: Long? = 0,
    @JsonProperty("txid")
    val txid: String? = "",
    @JsonProperty("version")
    val version: Long = 0,
    @JsonProperty("vin") @Valid
    val vin: List<Vin> = ArrayList(),
    @JsonProperty("vout") @Valid
    val vout: List<Vout> = ArrayList(),
    @JsonProperty("vsize")
    val vsize: Long? = 0
)
