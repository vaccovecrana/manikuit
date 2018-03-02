package unit

import com.onyx.persistence.factory.impl.EmbeddedPersistenceManagerFactory
import com.onyx.persistence.manager.PersistenceManager
import io.vacco.mk.base.*
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith

import j8spec.J8Spec.*
import org.junit.Assert.*
import com.onyx.persistence.query.*

@DefinedOrder
@RunWith(J8SpecRunner::class)
class PersistenceSpec {

  private val factory = EmbeddedPersistenceManagerFactory("/tmp/${this.javaClass.simpleName}")
  private var manager: PersistenceManager? = null
  private val txId = "0xDEADCAFEBABE"
  private val address = "0x12345"

  init {
    beforeAll {
      factory.initialize()
      manager = factory.persistenceManager
    }
    it("Can persist a payment summary.") {
      val r0 = MkPaymentRecord(
          address = address, amount = "0.001",
          txId = txId, blockHeight = 123456,
          type = MkExchangeRate.CryptoCurrency.ETH)
      val r1 = manager!!.saveEntity(r0)
      assertEquals(r1.txId, r0.txId)
      val r2: MkPaymentRecord = manager!!
          .from(MkPaymentRecord::class)
          .where("address" eq address).first()
      assertNotNull(r2)
      assertEquals(r2.address, r0.address)
    }
    it("Can persist a block record.") {
      val bid = "0ABCDEF"
      val b0 = MkBlock(
          id = bid, height = 1,
          hash = "BLOCKLOL12344", timeUtcSec = 1234567890,
          type = MkExchangeRate.CryptoCurrency.ETH)
      val b1 = manager!!.saveEntity(b0)
      assertEquals(b0.id, b1.id)
      val b2: MkBlock = manager!!
          .from(MkBlock::class)
          .where("id" eq bid).first()
      assertNotNull(b2)
      assertEquals(b1.id, b0.id)
    }
  }
}
