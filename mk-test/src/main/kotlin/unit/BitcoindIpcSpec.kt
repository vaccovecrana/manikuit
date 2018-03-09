package unit

import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.zeromq.ZMsg

@DefinedOrder
@RunWith(J8SpecRunner::class)
class BitcoindIpcSpec {

  private val log: Logger = LoggerFactory.getLogger(this.javaClass)

  init {
    it("Opens an IPC socket, and listens for messages.") {
      val ctx = ZContext()
      val client = ctx.createSocket(ZMQ.SUB)
      client.connect("tcp://127.0.0.1:28332")
      client.subscribe("hashtx")
      client.subscribe("hashblock")
      client.subscribe("rawblock")
      client.subscribe("rawtx")
      (0  .. 4).forEach {
        val msg = ZMsg.recvMsg(client)
        log.info(msg.toString())
      }
      ctx.close()
    }
  }

}
