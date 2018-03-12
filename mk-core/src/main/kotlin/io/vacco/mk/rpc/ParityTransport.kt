package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.base.eth.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.storage.MkBlockCache
import okhttp3.*
import java.math.*
import java.util.*

class ParityTransport(config: MkConfig, blockCache: MkBlockCache) : MkTransport(config, blockCache) {

  private val weiSize = 18
  private val roundHalfEven = RoundingMode.HALF_EVEN
  private val ethFactor = bd18(BigInteger.valueOf(10).pow(weiSize))
  private var webSocket: WebSocket? = client.newWebSocket(Request.Builder().url(config.pubSubUrl).build(),
      object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket?, response: Response?) {
          val msg = mapper.writeValueAsString(mapOf(
              "id" to UUID.randomUUID(), "jsonrpc" to "2.0", "method" to "eth_subscribe",
              "params" to arrayOf("logs", mapOf("fromBlock" to "latest", "toBlock" to "latest"))))
          webSocket!!.send(msg)
        }
        override fun onMessage(webSocket: WebSocket?, text: String?) {
          val payload = mapper.readTree(text)
          val tree = payload.path("params").path("result")
          if (tree.isArray && tree.size() > 0) {
            val firstTx = tree[0]
            val blockNoStr = firstTx.path("blockNumber").asText("")
            if (blockNoStr.isNotEmpty()) {
              val blockNo = decodeLong(blockNoStr)
              val blockSum = getBlock(blockNo)
              val blockDetail = getBlockDetail(blockSum)
              newBlock(blockDetail)
            }
          }
        }
      })

  override fun getChainType(): MkAccount.Crypto = MkAccount.Crypto.ETH
  override fun getUrl(payment: MkPaymentDetail): String = payment.account.address
  override fun getLatestBlockNumber(): Long = decodeLong(rpcRequest(String::class.java, "eth_blockNumber").second)

  override fun submitTransfer(payments: Collection<MkPaymentDetail>, targets: Collection<MkPaymentTarget>, unitFee: BigDecimal) {

  }

  override fun getBlock(height: Long): MkBlockSummary {
    val ethBlock = rpcRequest(EthBlock::class.java, "eth_getBlockByNumber",
        "0x${java.lang.Long.toHexString(height)}", false).second
    return MkBlockSummary(MkBlock(
        height = height, timeUtcSec = decodeLong(ethBlock.timestamp),
        hash = ethBlock.hash, type = MkAccount.Crypto.ETH
    ), ethBlock.transactions)
  }

  override fun getBlockDetail(summary: MkBlockSummary): MkBlockDetail {
    val ethBlock = rpcRequest(EthBlockDetail::class.java, "eth_getBlockByNumber",
        "0x${java.lang.Long.toHexString(summary.first.height)}", true).second
    val tx = ethBlock.transactions
        .filter { tx -> tx.to != null }
        .filter { tx -> decodeWei(tx.value) != BigInteger.ZERO }
        .map { tx -> MkPaymentRecord(
              type = MkAccount.Crypto.ETH, address = tx.to,
              txId = tx.hash, amount = tx.value, blockHeight = summary.first.height,
              outputIdx = 0, timeUtcSec = summary.first.timeUtcSec
          ) }
    return MkBlockDetail(summary.first, tx)
  }

  override fun doCreate(): Pair<String, String> {
    val addressPassPhrase = UUID.randomUUID().toString()
    val ethAddress = newAccount(addressPassPhrase)
    val accountData = mapper.writeValueAsString(exportAccount(ethAddress, addressPassPhrase))
    return Pair(ethAddress, "$accountData::$addressPassPhrase")
  }

  override fun close() { webSocket?.close(1000, "Transport is closing") }

  private fun netVersion(): Int = rpcRequest(Int::class.java, "net_version").second
  private fun protocolVersion(): Int = Integer.decode(rpcRequest(String::class.java, "eth_protocolVersion").second)
  private fun newAccount(passphrase: String): String = rpcRequest(String::class.java, "personal_newAccount", passphrase).second
  private fun exportAccount(address: String, passphrase: String): Map<*, *> = rpcRequest(
      Map::class.java, "parity_exportAccount", address, passphrase).second

  private fun decodeLong(input: String): Long = java.lang.Long.decode(input)
  private fun decodeWei(hexWei: String): BigInteger = BigInteger(hexWei.replace("0x", ""), 16)

  // Be careful with rounding errors in the input itself here... :/
  fun toEther(wei: BigInteger): BigDecimal = bd18(wei).divide(ethFactor, roundHalfEven)
  fun toWei(ether: BigDecimal): BigInteger = ether.multiply(ethFactor).setScale(weiSize, roundHalfEven).toBigInteger()
  fun bd18(amount: BigInteger): BigDecimal = BigDecimal(amount).setScale(weiSize, roundHalfEven)
}
