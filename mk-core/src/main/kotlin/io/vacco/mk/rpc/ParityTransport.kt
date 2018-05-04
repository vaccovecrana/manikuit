package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.base.eth.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.spi.MkBlockCache
import io.vacco.mk.util.MkSplit
import okhttp3.*
import java.math.*
import java.util.*

class ParityTransport(config: MkConfig, blockCache: MkBlockCache) : MkTransport(config, blockCache) {

  private var webSocket: WebSocket? = client.newWebSocket(Request.Builder().url(config.pubSubUrl).build(),
      object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket?, response: Response?) {
          val msg = mapper.writeValueAsString(mapOf(
              "id" to UUID.randomUUID(),
              "jsonrpc" to "2.0",
              "method" to "eth_subscribe",
              "params" to arrayOf("newHeads")))
          webSocket!!.send(msg)
        }
        override fun onMessage(webSocket: WebSocket?, text: String?) {
          if (log.isTraceEnabled) { log.trace("Raw WS message: [$text]") }
          val payload = mapper.readTree(text)
          val blockHash = payload.path("params").path("result").path("hash").asText("")
          if (blockHash != null && blockHash.isNotEmpty()) {
            val rawBlock = rpcRequest(EthBlock::class.java, "eth_getBlockByHash", blockHash, false).second
            if (rawBlock.transactions.isNotEmpty()) {
              if (log.isDebugEnabled) { log.debug("Retrieving block metadata for [$blockHash] with ${rawBlock.transactions.size} transactions") }
              val block = getBlock(decodeLong(rawBlock.number))
              val blockDetail = getBlockDetail(block)
              newBlock(blockDetail)
            }
          }
        }
      })

  override fun getChainType(): MkExchangeRate.Crypto = MkExchangeRate.Crypto.ETH
  override fun getCoinPrecision(): Int = 18
  override fun getFeeSplitMode(): MkSplit.FeeMode = MkSplit.FeeMode.PER_TARGET
  override fun getUrl(payment: MkPaymentDetail): String = payment.account.address
  override fun getLatestBlockNumber(): Long = decodeLong(rpcRequest(String::class.java, "eth_blockNumber").second)

  override fun getBlock(height: Long): MkBlockSummary {
    val ethBlock = rpcRequest(EthBlock::class.java, "eth_getBlockByNumber",
        "0x${java.lang.Long.toHexString(height)}", false).second
    return MkBlockSummary(MkBlock(
        height = height, timeUtcSec = decodeLong(ethBlock.timestamp),
        hash = ethBlock.hash, type = MkExchangeRate.Crypto.ETH
    ), ethBlock.transactions)
  }

  override fun doGetBlockDetail(summary: MkBlockSummary): MkBlockDetail {
    val ethBlock = rpcRequest(EthBlockDetail::class.java, "eth_getBlockByNumber",
        encodeLong(summary.first.height), true).second
    val tx = ethBlock.transactions
        .filter { tx -> tx.to != null }
        .filter { tx -> decodeHexInt(tx.value) != BigInteger.ZERO }
        .map { tx ->
          MkPaymentRecord(
              type = MkExchangeRate.Crypto.ETH, address = tx.to!!,
              txId = tx.hash!!, amount = tx.value, blockHeight = summary.first.height,
              outputIdx = 0, timeUtcSec = summary.first.timeUtcSec)
        }
    return MkBlockDetail(summary.first, tx)
  }

  override fun doBroadcast(source: MkPaymentDetail, targets: Collection<MkPaymentTarget>, unitFee: BigInteger): Collection<MkPaymentTarget> {
    requireNotNull(source)
    requireNotNull(targets)
    requireNotNull(unitFee)
    require(targets.isNotEmpty())
    if (!exists(source.account)) { import(source.account) }
    return targets.map { tg0 ->
      val rawTx = Transaction(from = source.account.address, to = tg0.address,
          gas = encodeHexInt(BigInteger.valueOf(21_000)),
          gasPrice = encodeHexInt(unitFee), value = encodeHexInt(tg0.amount))
      val txId = rpcRequest(String::class.java, "personal_sendTransaction",
          rawTx, decodeEntries(source.account)[1]).second
      tg0.copy(txId = txId)
    }
  }

  override fun computeFee(from: MkPaymentDetail, to: Collection<MkPaymentTarget>,
                          txUnitFee: BigInteger): BigInteger =
      txUnitFee.multiply(BigInteger.valueOf(21_000))

  override fun encodeAmount(amount: BigDecimal): String {
    val wei = amount.movePointRight(18)
    return encodeHexInt(wei.toBigInteger())
  }

  override fun decodeToUnit(rawAmount: String): BigInteger = decodeHexInt(rawAmount)

  override fun doCreate(): Pair<String, String> {
    val addressPassPhrase = UUID.randomUUID().toString()
    val ethAddress = newAccount(addressPassPhrase)
    val accountData = mapper.writeValueAsString(exportAccount(ethAddress, addressPassPhrase))
    return Pair(ethAddress, "$accountData$accountDataSep$addressPassPhrase")
  }

  override fun close() { webSocket?.close(1000, "Transport is closing") }

  private fun newAccount(passphrase: String): String = rpcRequest(String::class.java, "personal_newAccount", passphrase).second
  private fun exportAccount(address: String, passphrase: String): Map<*, *> = rpcRequest(
      Map::class.java, "parity_exportAccount", address, passphrase).second

  private fun exists(account: MkAccount): Boolean {
    val localAccount = rpcRequest(Array<String>::class.java, "personal_listAccounts")
        .second.find { it == account.address }
    return localAccount != null
  }

  private fun import(account: MkAccount): String {
    val acctData = decodeEntries(account)
    return rpcRequest(String::class.java, "parity_newAccountFromWallet", acctData[0], acctData[1]).second
  }

  private fun decodeEntries(account: MkAccount): Array<String> {
    requireNotNull(account)
    val rawData = MkAccountCodec.decode(account)
    val accountData = rawData.split(accountDataSep)
    require(accountData.size == 2)
    return accountData.toTypedArray()
  }

  fun decodeLong(input: String): Long = java.lang.Long.decode(input)
  fun encodeLong(input: Long): String = "0x${java.lang.Long.toHexString(input)}"
  fun decodeHexInt(hexInt: String): BigInteger = BigInteger(hexInt.replace("0x", ""), 16)
  fun encodeHexInt(int: BigInteger): String = "0x${int.toString(16)}"
}
