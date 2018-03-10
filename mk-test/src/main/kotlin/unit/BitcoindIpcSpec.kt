package unit

import com.onyx.persistence.factory.impl.EmbeddedPersistenceManagerFactory
import com.onyx.persistence.manager.PersistenceManager
import io.vacco.mk.config.MkConfig
import io.vacco.mk.rpc.BitcoindTransport
import io.vacco.mk.storage.MkBlockCache
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.*

import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.zeromq.ZMsg
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class BitcoindIpcSpec {

  private val factory = EmbeddedPersistenceManagerFactory("/tmp/${this.javaClass.simpleName}")
  private var manager: PersistenceManager? = null
  private var btc: BitcoindTransport? = null

  init {
    beforeAll {
      factory.initialize()
      manager = factory.persistenceManager
      val cfg = MkConfig(6, 1, ChronoUnit.HOURS, 10, TimeUnit.SECONDS)
      cfg.rootUrl = "http://127.0.0.1:18332"
      cfg.username = "gopher"
      cfg.password = "omglol"
      cfg.connectionPoolSize = 8
      btc = BitcoindTransport(cfg, MkBlockCache(manager!!))
    }

    it("Opens an IPC socket, listens and forwards messages.") {
      val ctx = ZContext()
      val client = ctx.createSocket(ZMQ.SUB)
      client.connect("tcp://127.0.0.1:28332")
      client.subscribe("hashtx")
      client.subscribe("hashblock")
      client.subscribe("rawblock")
      client.subscribe("rawtx")
      (0  .. 1024).forEach {
        val msg = ZMsg.recvMsg(client)
        btc!!.opPubSubMessage(msg)
      }
      ctx.close()
    }
  }
}
