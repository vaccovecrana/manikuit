package io.vacco.mk.rpc

import com.fasterxml.jackson.databind.JsonNode
import io.vacco.mk.base.*
import io.vacco.mk.base.btc.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.spi.MkBlockCache
import io.vacco.mk.util.MkHashing
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

  override fun doGetBlockDetail(height: Long): MkBlockDetail {
    val btcBlockHash = rpcRequest(String::class.java, "getblockhash", height).second
    return convert(getBtcBlock(btcBlockHash))
  }

  private fun convert(btcBlock: BtcBlock): MkBlockDetail {
    val mkBlock = MkBlock(
        id = MkHashing.apply(MkExchangeRate.Crypto.BTC, btcBlock.height, btcBlock.hash),
        height = btcBlock.height, timeUtcSec = btcBlock.time, hash = btcBlock.hash,
        type = MkExchangeRate.Crypto.BTC)
    val tx = btcBlock.tx
        .filter { tx -> tx.vout.isNotEmpty() }
        .flatMap { tx -> tx.vout.map { out -> BtcOut(tx, out) } }
        .filter { txout -> txout.second.value > 0 }
        .filter { txout -> txout.second.scriptPubKey.reqSigs > 0 }
        .filter { txout -> txout.second.scriptPubKey.addresses != null }
        .filter { txout -> txout.second.scriptPubKey.addresses!!.isNotEmpty() }
        .flatMap { txout -> txout.second.scriptPubKey.addresses!!.map { addr -> BtcAddrOut(txout, addr) } }
        .map { addrOut ->
          MkPaymentRecord(
              type = MkExchangeRate.Crypto.BTC, address = addrOut.second,
              txId = addrOut.first.first.txid!!, amount = df.get()!!.format(addrOut.first.second.value),
              blockHeight = mkBlock.height, outputIdx = addrOut.first.second.n,
              timeUtcSec = btcBlock.time)
        }
    return MkBlockDetail(mkBlock, tx)
  }

  override fun doBroadcast(source: MkPaymentDetail, targets: Collection<MkPaymentTarget>,
                           unitFee: BigInteger): Collection<MkPaymentTarget> {
    requireNotNull(source)
    requireNotNull(targets)
    require(targets.isNotEmpty())
    requireNotNull(unitFee)
    val rawTx = createRawTx(source, targets)
    val signedTx = signRawTx(source, rawTx)
    val txId = rpcRequest(String::class.java, "sendrawtransaction", signedTx.hex!!).second
    return targets.map { it.copy(txId = txId) }
  }

  override fun doCreate(): Pair<String, String> {
    val address = getNewAddress()
    return Pair(address.first, address.second)
  }

  override fun encodeAmount(amount: BigDecimal): String = amount.setScale(8).toString()

  override fun decodeToUnit(rawAmount: String): BigInteger = toSatoshi(rawAmount)

  override fun close() { zmqHandler?.cancel(NotActiveException("Transport is closing")) }

  private fun getNewAddress(): Pair<String, String> {
    val address = rpcRequest(String::class.java, "getnewaddress").second
    var privateKey = rpcRequest(String::class.java, "dumpprivkey", address).second
    if (isMultiSig(address)) { // need to save segwit/multisig redeemScript
      var redeemScript = getRedeemScript(address, "getaddressinfo")
      if (redeemScript == null) {
        redeemScript = getRedeemScript(address, "validateaddress")
      }
      requireNotNull(redeemScript)
      privateKey = "$privateKey$accountDataSep$redeemScript"
    }
    return Pair(address, privateKey)
  }

  private fun getRedeemScript(address: String, tryMethod: String): String? {
    try {
      val info = rpcRequest(JsonNode::class.java, tryMethod, address)
      return info.second.path("hex").textValue()
    } catch (e: Exception) {
      if (log.isTraceEnabled) { log.trace("Cannot extract address redeem script using [$tryMethod]") }
    }
    return null
  }

  private fun getBtcBlock(hash: String): BtcBlock = rpcRequest(BtcBlock::class.java, "getblock", hash, 2).second

  private fun getTransaction(txId: String): BtcTx? = try {
    rpcRequest(BtcTx::class.java, "getrawtransaction", txId, 1).second
  } catch (e: Exception) { null }

  private fun decodeRawTransaction(txHex: String): BtcTx {
    val tx = rpcRequest(BtcTx::class.java, "decoderawtransaction", txHex).second
    return tx.copy(hex = txHex)
  }

  private fun createRawTx(from: MkPaymentDetail, to: Collection<MkPaymentTarget>): BtcTx {
    requireNotNull(from)
    requireNotNull(to)
    require(to.isNotEmpty())
    val btcVin = Vin(txid = from.record.txId, vout = from.record.outputIdx)
    val targets = to.map { (it.address to toBtc(it.amount).toString()) }.toTypedArray()
    val txHex = rpcRequest(String::class.java, "createrawtransaction", arrayOf(btcVin), mapOf(*targets)).second
    return decodeRawTransaction(txHex)
  }

  private fun signRawTx(from: MkPaymentDetail, tx: BtcTx): BtcTx {
    requireNotNull(tx)
    requireNotNull(tx.hex)
    requireNotNull(from)
    val prevTx = requireNotNull(getTransaction(from.record.txId))
    val txo = requireNotNull(prevTx.vout.find { it.n == from.record.outputIdx })
    val txoParams = BtcTxoParams(from.record.txId, from.record.outputIdx,
        txo.scriptPubKey.hex, from.record.amount, null)
    val prvData = MkAccountCodec.decode(from.account).split(accountDataSep)
    if (isMultiSig(from.account.address)) {
      txoParams.redeemScript = prvData[1]
    }
    val result = rpcRequest(Map::class.java, "signrawtransaction", tx.hex!!,
        arrayOf(txoParams), arrayOf(prvData[0])).second
    return decodeRawTransaction(result.get("hex") as String)
  }

  override fun computeFee(from: MkPaymentDetail, to: Collection<MkPaymentTarget>, txUnitFee: BigInteger): BigInteger {
    val rawTx = createRawTx(from, to)
    return BigInteger.valueOf(rawTx.size).multiply(txUnitFee)
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
          if (log.isDebugEnabled) { log.debug("Zmq BTC block frame: [$msg]") }
          val btcBlock = getBtcBlock(msg.popString())
          val blockDetail = convert(btcBlock)
          newBlock(blockDetail)
        }
      }
    }
  }

  private fun isMultiSig(address: String) = address.startsWith("2") || address.startsWith("3")
}