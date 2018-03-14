package unit

import com.onyx.persistence.factory.impl.EmbeddedPersistenceManagerFactory
import com.onyx.persistence.manager.PersistenceManager
import io.vacco.mk.base.*
import io.vacco.mk.config.MkConfig
import io.vacco.mk.rpc.BitcoindTransport
import io.vacco.mk.storage.MkBlockCache
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.*
import org.slf4j.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class BitcoindTransferSpec {

  private val log: Logger = LoggerFactory.getLogger(this.javaClass)

  private val factory = EmbeddedPersistenceManagerFactory("/tmp/${this.javaClass.simpleName}")
  private var manager: PersistenceManager? = null
  private var btc: BitcoindTransport? = null

  private val seedAddress = MkAccount(
      MkExchangeRate.Crypto.BTC,
      "mxm1LEvPouNepgHynTsggu88pP1q1SNSTZ",
      "qUGEUl9g9qYkGtYviPPXRIZ0YJLa0pigU+q2lmuiEhRx3Au0zUStzmYnr5+h7AwqG0UrEgPgBzXJsGFyBRH++LO7QRU=",
      "mjIDe9MAUVt58Gng", System.getProperty("manikuit.test.seedGcmKey")
  ) // the seed address must contain at least 1BTC.

  private var oneToOneTarget: MkAccount? = null

  init {
    beforeAll {
      factory.initialize()
      manager = factory.persistenceManager
      val cfg = MkConfig(6, 1, ChronoUnit.HOURS, 10, TimeUnit.SECONDS)
      cfg.pubSubUrl = "tcp://127.0.0.1:28332"
      cfg.rootUrl = "http://127.0.0.1:18332"
      cfg.username = "gopher"
      cfg.password = "omglol"
      cfg.connectionPoolSize = 8
      btc = BitcoindTransport(cfg, MkBlockCache(manager!!))
    }
    it("Transfers 0.1 BTC to a new account (1 to 1 transfer)") {
      oneToOneTarget = btc!!.create()
      val paymentIn = MkPaymentDetail(seedAddress, MkPaymentRecord("lol", MkExchangeRate.Crypto.BTC,
          seedAddress.address, "0123456789ABC", "0.01", 0, 0))

    }
  }
}
