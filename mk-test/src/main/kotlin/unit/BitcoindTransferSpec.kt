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
import org.junit.Assert.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class BitcoindTransferSpec {

  private val log: Logger = LoggerFactory.getLogger(javaClass)

  private val factory = EmbeddedPersistenceManagerFactory("/tmp/${this.javaClass.simpleName}")
  private var manager: PersistenceManager? = null
  private var btc: BitcoindTransport? = null

  private var oneToOneTarget: MkAccount? = MkAccount(
      crypto=MkExchangeRate.Crypto.BTC,
      address="misV7bW9EM67X3r8ENv7hYnsk2eppvQxf6",
      cipherText="9Co61QQGLc6eYGkI+CVcB3B7wObPXlF6Pkw2mLPAk5LlTlAQ086z2IiY6zVJ1oyOTzfwM7/1Y3UqGCtlXmP4L7i6xrI=",
      iv="xfj7HcNoKU45TvO3",
      gcmKey="UpNq7gNLhPfqzT7e+S3axHTLYZnHoOo4fplf8dWBAIk="
  )
  private val seedAmount = "0.03000000"
  private var seedPayment: MkPaymentRecord? = null

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
    it("Transfers 0.01 BTC to a new account (1 to 1 transfer)") {
      // oneToOneTarget = btc!!.create()
      ProcessBuilder("/bin/bash", "-c",
          "qrencode -o - bitcoin:${oneToOneTarget!!.address}?amount=$seedAmount | open -f -a preview"
      ).start()
      btc!!.update()
      log.info("Send me coin! :D")
      while (true) {
        print(".")
        seedPayment = btc!!.getPaymentsFor(oneToOneTarget!!.address).firstOrNull { it.amount == seedAmount }
        if (seedPayment != null) break
        Thread.sleep(60_000)
      }
      println()
      assertNotNull(seedPayment)
    }
  }
}
