package util

import io.vacco.mk.base.MkPaymentRecord
import io.vacco.mk.rpc.MkTransport
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object MkPaymentUtil {

  private val log: Logger = LoggerFactory.getLogger(javaClass)

  fun awaitPayment(address: String, txId: String?, amount: String?, trx: MkTransport): MkPaymentRecord {
    var py0: MkPaymentRecord? = null
    while (true) {
      log.info("Awaiting payment on [$address, $txId, $amount]")
      val addrTx = trx.getPaymentsFor(address)
      if (amount == null) {
        py0 = addrTx.firstOrNull { it.txId == txId }
      } else if (txId == null) {
        py0 = addrTx.firstOrNull { it.amount == amount }
      }
      if (py0 != null) break
      Thread.sleep(30_000)
    }
    return py0!!
  }

  fun awaitConfirmation(payment: MkPaymentRecord, trx: MkTransport): MkPaymentRecord.Status {
    while (true) {
      log.info("Awaiting payment confirmation on [$payment]")
      val currentBlock = trx.getLatestBlockNumber()
      val status = trx.getStatus(payment, currentBlock)
      if (status == MkPaymentRecord.Status.COMPLETE) break
      Thread.sleep(30_000)
    }
    return MkPaymentRecord.Status.COMPLETE
  }

}