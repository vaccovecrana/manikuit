package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.util.SecretUtils
import io.vacco.mk.storage.MkBlockCache
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class GethTransport(config: MkConfig,
                    blockCache: MkBlockCache): MkTransport(config, blockCache) {

  private val weiSize = 18
  private val roundHalfEven = RoundingMode.HALF_EVEN
  private val ethFactor = bd18(BigInteger.valueOf(10).pow(weiSize))

  override fun getLatestBlockNumber(): Long {
    return decodeLong(rpcRequest(String::class.java, "eth_blockNumber").second)
  }

  override fun getBlock(height: Long): CgBlockSummary {
    val ethBlock = rpcRequest(EthBlock::class.java, "eth_getBlockByNumber",
        "0x${java.lang.Long.toHexString(height)}", false).second
    return CgBlockSummary(MkBlock(
        height = height, timeUtcSec = decodeLong(ethBlock.timestamp),
        hash = ethBlock.hash, type = MkExchangeRate.CryptoCurrency.ETH
    ), ethBlock.transactions)
  }

  override fun getBlockDetail(summary: CgBlockSummary): CgBlockDetail {
    val ethBlock = rpcRequest(EthBlockDetail::class.java, "eth_getBlockByNumber",
        "0x${java.lang.Long.toHexString(summary.first.height)}", true).second
    val tx = ethBlock.transactions
        .filter { tx -> tx.to != null }
        .filter { tx -> decodeWei(tx.value) != BigInteger.ZERO }
        .map { tx -> MkPaymentRecord(
            type = MkExchangeRate.CryptoCurrency.ETH, address = tx.to,
            txId = tx.hash, amount = tx.value, blockHeight = summary.first.height,
            timeUtcSec = summary.first.timeUtcSec
        ) }
    return CgBlockDetail(summary.first, tx)
  }

  override fun create(rawSecret: String?, secretParts: Int, secretRequired: Int): MkPayment {
    val ethAddress = newAccount(rawSecret!!)
    return MkPayment()
        .withType(MkExchangeRate.CryptoCurrency.ETH)
        .withAddress(ethAddress)
        .withSecretParts(SecretUtils.split(rawSecret, secretParts, secretRequired))
  }

  private fun netVersion(): Int = rpcRequest(Int::class.java, "net_version").second
  private fun protocolVersion(): Int = Integer.decode(rpcRequest(String::class.java, "eth_protocolVersion").second)
  private fun newAccount(passphrase: String): String = rpcRequest(String::class.java, "personal_newAccount", passphrase).second

  override fun getChainType(): MkExchangeRate.CryptoCurrency = MkExchangeRate.CryptoCurrency.ETH
  private fun decodeLong(input: String): Long = java.lang.Long.decode(input)
  private fun decodeWei(hexWei: String): BigInteger = BigInteger(hexWei.replace("0x", ""), 16)

  // Be careful with rounding errors in the input itself here... :/
  fun toEther(wei: BigInteger): BigDecimal = bd18(wei).divide(ethFactor, roundHalfEven)
  fun toWei(ether: BigDecimal): BigInteger = ether.multiply(ethFactor).setScale(weiSize, roundHalfEven).toBigInteger()
  fun bd18(amount: BigInteger): BigDecimal = BigDecimal(amount).setScale(weiSize, roundHalfEven)
}
