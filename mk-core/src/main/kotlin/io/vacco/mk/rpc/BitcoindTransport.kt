package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.base.btc.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.storage.MkBlockCache
import io.vacco.mk.util.MkSplit
import kotlinx.coroutines.experimental.*
import org.zeromq.*
import java.io.NotActiveException
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat

typealias BtcOut = Pair<BtcTx, Vout>
typealias BtcAddrOut = Pair<BtcOut, String>

class BitcoindTransport(config: MkConfig, blockCache: MkBlockCache): MkTransport(config, blockCache) {

  private val btcScale = 8
  private val satoshiFactor = BigDecimal.TEN.setScale(btcScale).pow(btcScale)
  private val df: ThreadLocal<DecimalFormat> = ThreadLocal.withInitial { DecimalFormat("#0.00000000") }
  private val ctx = ZContext()
  private val zmqClient = ctx.createSocket(ZMQ.SUB)
  private var zmqHandler: Deferred<Unit>? = null

  private val hashTx = "hashtx"
  private val hashBlock = "hashblock"

  init {
    zmqClient.connect(config.pubSubUrl)
    zmqClient.subscribe(hashBlock)
    zmqClient.subscribe(hashTx)
    zmqHandler = async { while (true) { onZmqMessage(ZMsg.recvMsg(zmqClient)) } }
  }

  override fun getChainType(): MkExchangeRate.Crypto = MkExchangeRate.Crypto.BTC
  override fun getCoinPrecision(): Int = btcScale
  override fun getFeeSplitMode(): MkSplit.FeeMode = MkSplit.FeeMode.PER_TRANSACTION
  override fun getUrl(payment: MkPaymentDetail): String = "bitcoin:${payment.account.address}?amount=${payment.record.amount}"
  override fun getLatestBlockNumber(): Long = rpcRequest(Long::class.java, "getblockcount").second

  override fun getBlock(height: Long): MkBlockSummary {
    val btcBlockHash = rpcRequest(String::class.java, "getblockhash", height).second
    val btcBlock = getBtcBlock(btcBlockHash)
    return Pair(MkBlock(height = height, timeUtcSec = btcBlock.time,
        hash = btcBlockHash, type = MkExchangeRate.Crypto.BTC), btcBlock.tx.toList())
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
              type = MkExchangeRate.Crypto.BTC, address = addrOut.second,
              txId = addrOut.first.first.txid, amount = df.get()!!.format(addrOut.first.second.value),
              blockHeight = summary.first.height, outputIdx = addrOut.first.second.n,
              timeUtcSec = addrOut.first.first.time)
          }
    }
    return MkBlockDetail(summary.first, tx)
  }

  override fun doBroadcast(source: MkPaymentDetail, targets: Collection<MkPaymentTarget>, unitFee: BigInteger): String {
    requireNotNull(source)
    requireNotNull(targets)
    require(targets.isNotEmpty())
    requireNotNull(unitFee)
    val rawTx = createRawTx(source, targets)
    val signedTx = signRawTx(source, rawTx)
    return rpcRequest(String::class.java, "sendrawtransaction", signedTx.hex).second
  }

  override fun doCreate(): Pair<String, String> {
    val address = getNewAddress()
    return Pair(address.first, address.second)
  }

  override fun decodeToUnit(rawAmount: String): BigInteger = toSatoshi(rawAmount)

  override fun close() { zmqHandler?.cancel(NotActiveException("Transport is closing")) }

  private fun getNewAddress(): Pair<String, String> {
    val address = rpcRequest(String::class.java, "getnewaddress").second
    val privateKey = rpcRequest(String::class.java, "dumpprivkey", address).second
    return Pair(address, privateKey)
  }

  private fun getBtcBlock(hash: String): BtcBlock = rpcRequest(BtcBlock::class.java, "getblock", hash).second

  private fun getTransaction(txId: String): BtcTx? {
    try { return rpcRequest(BtcTx::class.java, "getrawtransaction", txId, 1).second }
    catch (e: Exception) { log.error(e.message) }
    return null
  }

  private fun decodeRawTransaction(txHex: String): BtcTx {
    val tx = rpcRequest(BtcTx::class.java, "decoderawtransaction", txHex).second
    return tx.withHex(txHex)
  }

  fun createRawTx(from: MkPaymentDetail, to: Collection<MkPaymentTarget>): BtcTx {
    requireNotNull(from)
    requireNotNull(to)
    require(to.isNotEmpty())
    val btcVin = Vin().withTxid(from.record.txId).withVout(from.record.outputIdx)
    val targets = to.map { (it.address to toBtc(it.amount).toString()) }.toTypedArray()
    val txHex = rpcRequest(String::class.java, "createrawtransaction", arrayOf(btcVin), mapOf(*targets)).second
    return decodeRawTransaction(txHex)
  }

  fun signRawTx(from: MkPaymentDetail, tx: BtcTx): BtcTx {
    requireNotNull(tx)
    requireNotNull(tx.hex)
    requireNotNull(from)
    val prevTx = requireNotNull(getTransaction(from.record.txId))
    val txo = requireNotNull(prevTx.vout.find { it.n == from.record.outputIdx })
    val txoParams = BtcTxoParams(from.record.txId, from.record.outputIdx, txo.scriptPubKey.hex)
    val result = rpcRequest(Map::class.java, "signrawtransaction", tx.hex,
        arrayOf(txoParams), arrayOf(decode(from.account))).second
    return decodeRawTransaction(result.get("hex") as String)
  }

  fun toBtc(satoshi: BigInteger): BigDecimal = satoshi.toBigDecimal()
      .setScale(getCoinPrecision()).divide(satoshiFactor)

  fun toSatoshi(btc: String): BigInteger = BigDecimal(btc)
      .setScale(getCoinPrecision()).multiply(satoshiFactor).toBigInteger()

  private fun onZmqMessage(msg: ZMsg) {
    if (log.isTraceEnabled) { log.trace("Zmq frame: [$msg]") }
    if (msg.size == 3) {
      val topic = msg.popString()
      when (topic) {
        hashBlock -> {
          val btcBlock = getBtcBlock(msg.popString())
          val blockSummary = getBlock(btcBlock.height)
          val blockDetail = getBlockDetail(blockSummary)
          newBlock(blockDetail)
        }
      }
    }
  }
}