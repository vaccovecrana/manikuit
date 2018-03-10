package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.base.btc.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.storage.MkBlockCache
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.zeromq.ZMsg
import java.math.BigDecimal
import java.text.DecimalFormat

typealias BtcOut = Pair<BtcTx, Vout>
typealias BtcAddrOut = Pair<BtcOut, String>

class BitcoindTransport(config: MkConfig,
                        blockCache: MkBlockCache): MkTransport<ZMsg>(config, blockCache) {

  private val df: ThreadLocal<DecimalFormat> = ThreadLocal.withInitial { DecimalFormat("#0.00000000") }

  override fun getUrl(account: MkAccount): String = "bitcoin:${account.address}?amount=${account.amount}"

  override fun getLatestBlockNumber(): Long = rpcRequest(Long::class.java, "getblockcount").second

  override fun transfer(payments: Collection<MkPaymentDetail>, targets: Collection<MkPaymentTarget>, unitFee: BigDecimal) {

  }

  override fun opPubSubMessage(payload: ZMsg) {
    requireNotNull(payload)
    require(payload.size == 3)
    val topic = payload.popString()
    when (topic) {
      "hashblock" -> {
        val blockHash = payload.popString()
        val btcBlock = getBtcBlock(blockHash)
        val blockSummary = getBlock(btcBlock.height)
        val blockDetail = getBlockDetail(blockSummary)
        newBlock(blockDetail)
      }
      "hashtx" -> {
        if (log.isTraceEnabled) {
          log.trace("New tx hash [${payload.popString()}]")
        }
      }
    }
  }

  override fun getBlock(height: Long): MkBlockSummary {
    val btcBlockHash = rpcRequest(String::class.java, "getblockhash", height).second
    val btcBlock = getBtcBlock(btcBlockHash)
    return Pair(
        MkBlock(height = height, timeUtcSec = btcBlock.time,
            hash = btcBlockHash, type = MkAccount.Crypto.BTC
        ), btcBlock.tx.toList()
    )
  }

  override fun getBlockDetail(summary: MkBlockSummary): MkBlockDetail {
    val deferred = summary.second.map { txId -> async { getTransaction(txId) } }
    val tx = runBlocking {
      deferred.map { it.await() }
          .filter { tx -> tx != null }
          .filter { tx -> tx!!.vout.isNotEmpty() }
          .flatMap { tx -> tx!!.vout.map { out -> BtcOut(tx, out) } }
          .filter { txout -> txout.second.value > 0 }
          .filter { txout -> txout.second.scriptPubKey != null }
          .filter { txout -> txout.second.scriptPubKey.reqSigs > 0 }
          .filter { txout -> txout.second.scriptPubKey.addresses.isNotEmpty() }
          .flatMap { txout -> txout.second.scriptPubKey.addresses.map { addr -> BtcAddrOut(txout, addr) } }
          .map { addrOut -> MkPaymentRecord(
              type = MkAccount.Crypto.BTC, address = addrOut.second,
              txId = addrOut.first.first.txid, amount = df.get()!!.format(addrOut.first.second.value),
              blockHeight = summary.first.height, outputIdx = addrOut.first.second.n,
              timeUtcSec = addrOut.first.first.time)
          }
    }
    return MkBlockDetail(summary.first, tx)
  }

  override fun doCreate(): Pair<MkAccount, String> {
    val address = getNewAddress()
    return Pair(MkAccount()
        .withAddress(address.first)
        .withCrypto(MkAccount.Crypto.BTC), address.second)
  }

  private fun getNewAddress(): Pair<String, String> {
    val address = rpcRequest(String::class.java, "getnewaddress").second
    val privateKey = rpcRequest(String::class.java, "dumpprivkey", address).second
    return Pair(address, privateKey)
  }

  override fun getChainType(): MkAccount.Crypto = MkAccount.Crypto.BTC

  private fun getBtcBlock(hash: String): BtcBlock = rpcRequest(BtcBlock::class.java, "getblock", hash).second

  private fun getTransaction(txId: String): BtcTx? {
    try { return rpcRequest(BtcTx::class.java, "getrawtransaction", txId, 1).second }
    catch (e: Exception) { log.error(e.message) }
    return null
  }

  fun getTxOut(txId: String, vout: Long): Any? {
    try { return rpcRequest(Any::class.java, "gettxout", txId, vout) }
    catch (e: Exception) { log.error(e.message) }
    return null
  }
}