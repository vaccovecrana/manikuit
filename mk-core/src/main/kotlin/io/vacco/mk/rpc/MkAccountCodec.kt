package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.util.*
import org.slf4j.*
import java.nio.ByteBuffer
import java.util.*

object MkAccountCodec {

  val log: Logger = LoggerFactory.getLogger(javaClass)

  fun decode(account: MkAccount): String {
    requireNotNull(account.cipherText)
    requireNotNull(account.iv)
    requireNotNull(account.gcmKey)
    val dec = Base64.getDecoder()
    return String(GcmCrypto.decryptGcm(
        Ciphertext(dec.decode(account.cipherText),
            dec.decode(account.iv)), dec.decode(account.gcmKey)), Charsets.UTF_8)
  }

  fun encodeRaw(address: String, pData: String, type: MkExchangeRate.Crypto): MkAccount {
    requireNotNull(address)
    requireNotNull(pData)
    val key = GcmCrypto.generateKey(256)
    val encoded = GcmCrypto.encryptGcm(pData.toByteArray(), key)
    val b64Enc = Base64.getEncoder()
    val account = MkAccount(type, address,
        b64Enc.encodeToString(encoded.ciphertext),
        b64Enc.encodeToString(encoded.iv),
        b64Enc.encodeToString(key))
    if (log.isTraceEnabled) log.trace(account.toString())
    return account
  }

  fun fingerPrintAddress(pr: MkPaymentRecord): ByteBuffer {
    val data = StringBuilder().append(pr.type).append(pr.address.toLowerCase()).toString()
    val bytes = data.toByteArray()
    val bb = ByteBuffer.allocateDirect(bytes.size)
    bb.put(bytes)
    bb.clear()
    return bb
  }
}