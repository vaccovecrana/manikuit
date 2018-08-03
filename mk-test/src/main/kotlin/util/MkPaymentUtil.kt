package util

import io.vacco.mk.base.MkPaymentRecord
import io.vacco.mk.rpc.MkTransport
import org.slf4j.*
import java.util.concurrent.ConcurrentHashMap

object MkPaymentUtil {

  private val log: Logger = LoggerFactory.getLogger(javaClass)

  fun awaitPayment(trx: MkTransport, vararg address: String):
      Map<String, MkPaymentRecord> {
    val result = ConcurrentHashMap<String, MkPaymentRecord>()
    address.map {
      trx.notifyOnAddress(MkPaymentRecord(type = trx.getChainType(), address = it))
    }
    trx.onAddressMatch = {
      result[it.address] = it
    }
    while (result.size != address.size) {
      log.info("Awaiting payments: [${result.size} of ${address.size}] on [${address.joinToString()}]")
      Thread.sleep(10_000)
    }
    return result
  }

  fun awaitConfirmation(payment: MkPaymentRecord, trx: MkTransport): MkPaymentRecord {
    var status: MkPaymentRecord.Status
    while (true) {
      status = trx.getStatus(payment, trx.getLatestBlockNumber())
      if (status == MkPaymentRecord.Status.COMPLETE) { break }
      log.info("Awaiting payment confirmation on $payment")
      Thread.sleep(10_000)
    }
    log.info("Payment confirmed: ${payment}")
    return payment
  }
}