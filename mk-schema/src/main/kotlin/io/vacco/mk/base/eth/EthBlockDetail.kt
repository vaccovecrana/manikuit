package io.vacco.mk.base.eth

import java.util.ArrayList
import java.util.HashMap
import javax.validation.Valid
import com.fasterxml.jackson.annotation.*

/**
 * ETH block with transaction detail.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("transactions")
data class EthBlockDetail(
    @JsonProperty("transactions")
    @Valid val transactions: List<Transaction> = ArrayList()
)
