package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.util.SecretUtils
import io.vacco.mk.storage.MkBlockCache
import org.slf4j.*
import java.text.DecimalFormat

typealias BtcOut = Pair<BtcTx, BtcVout>
typealias BtcAddrOut = Pair<BtcOut, String>

class BitcoindTransport(config: MkConfig,
                        blockCache: MkBlockCache): MkTransport(config, blockCache) {

  private val log: Logger = LoggerFactory.getLogger(javaClass)
  private val df: ThreadLocal<DecimalFormat> = ThreadLocal.withInitial { DecimalFormat("#0.00000000") }

  override fun getLatestBlockNumber(): Long {
    return rpcRequest(Long::class.java, "getblockcount").value
  }

  override fun getBlock(height: Long): CgBlockSummary {
    val btcBlockHash = rpcRequest(String::class.java, "getblockhash", height).value
    val btcBlock = getBtcBlock(btcBlockHash)
    return Pair(
        MkBlock(height = height, timeUtcSec = btcBlock.time,
            hash = btcBlockHash, type = MkExchangeRate.CryptoCurrency.BTC
        ), btcBlock.tx.toList()
    )
  }

  override fun getBlockDetail(summary: CgBlockSummary): CgBlockDetail {
    val tx = summary.second.map(this::getTransaction)
        .filter { tx -> tx != null }
        .filter { tx -> tx!!.vout.isNotEmpty() }
        .flatMap { tx -> tx!!.vout.map { out -> BtcOut(tx, out) } }
        .filter { txout -> txout.second.value > 0 }
        .filter { txout -> txout.second.scriptPubKey != null }
        .filter { txout -> txout.second.scriptPubKey.reqSigs > 0 }
        .filter { txout -> txout.second.scriptPubKey.addresses.isNotEmpty() }
        .flatMap { txout -> txout.second.scriptPubKey.addresses.map { addr -> BtcAddrOut(txout, addr) } }
        .map { addrOut -> MkPaymentRecord(
            type = MkExchangeRate.CryptoCurrency.BTC, address = addrOut.second,
            txId = addrOut.first.first.txid, amount = df.get()!!.format(addrOut.first.second.value),
            blockHeight = summary.first.height, timeUtcSec = addrOut.first.first.time)
        }
    return CgBlockDetail(summary.first, tx)
  }

  override fun create(rawSecret: String?, secretParts: Int, secretRequired: Int): MkPayment {
    val address = getNewAddress()
    return MkPayment()
        .withAddress(address.p2pKh)
        .withType(MkExchangeRate.CryptoCurrency.BTC)
        .withSecretParts(SecretUtils.split(address.privateKey, secretParts, secretRequired))
  }

  private fun getNewAddress(): BtcAddress {
    val address = rpcRequest(String::class.java, "getnewaddress").value
    val privateKey = rpcRequest(String::class.java, "dumpprivkey", address).value
    return BtcAddress().withP2pKh(address).withPrivateKey(privateKey)
  }

  override fun getChainType(): MkExchangeRate.CryptoCurrency = MkExchangeRate.CryptoCurrency.BTC

  private fun getBtcBlock(hash: String): BtcBlock = rpcRequest(BtcBlock::class.java, "getblock", hash).value

  private fun getTransaction(txId: String): BtcTx? {
    try {
      return rpcRequest(BtcTx::class.java, "getrawtransaction", txId, 1).value
    } catch (e: Exception) { log.error(e.message) }
    return null
  }
}