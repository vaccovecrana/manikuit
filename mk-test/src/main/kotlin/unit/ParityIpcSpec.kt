package unit

import com.onyx.persistence.factory.impl.EmbeddedPersistenceManagerFactory
import com.onyx.persistence.manager.PersistenceManager
import io.vacco.mk.config.MkConfig
import io.vacco.mk.rpc.ParityTransport
import io.vacco.mk.storage.MkBlockCache
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.*
import okhttp3.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class ParityIpcSpec : WebSocketListener() {

  private val factory = EmbeddedPersistenceManagerFactory("/tmp/${this.javaClass.simpleName}")
  private var manager: PersistenceManager? = null
  private var eth: ParityTransport? = null

  init {
    beforeAll {
      factory.initialize()
      manager = factory.persistenceManager
      val cfg = MkConfig(12, 1, ChronoUnit.HOURS, 60, TimeUnit.SECONDS)
      cfg.pubSubUrl = "ws://127.0.0.1:8546"
      cfg.rootUrl = "http://127.0.0.1:8545"
      eth = ParityTransport(cfg, MkBlockCache(manager!!))
    }

    it("Opens a new websocket Parity connection and processes incoming messages") {
      Thread.sleep(40000)
      eth?.close()
    }
  }
}

