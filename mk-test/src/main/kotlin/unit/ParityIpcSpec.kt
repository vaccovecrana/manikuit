package unit

import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.*
import okhttp3.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

@DefinedOrder
@RunWith(J8SpecRunner::class)
class ParityIpcSpec : WebSocketListener() {

  private val log: Logger = LoggerFactory.getLogger(this.javaClass)

  val client: OkHttpClient? = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
  var webSocket: WebSocket? = null

  init {
    it("Opens a new websocket Parity connection") {
      webSocket = client!!.newWebSocket(Request.Builder().url("ws://127.0.0.1:8546").build(), this)
      Thread.sleep(20000)
      webSocket!!.close(1000, "Goodbye!")
      client.dispatcher().executorService().shutdown()
    }
  }

  override fun onOpen(webSocket: WebSocket?, response: Response?) {
    webSocket!!.send("{\"method\":\"eth_subscribe\",\"params\":[\"logs\",{\"fromBlock\":\"latest\",\"toBlock\":\"latest\"}],\"id\":1,\"jsonrpc\":\"2.0\"}")
  }

  override fun onMessage(webSocket: WebSocket?, text: String?) {
    log.info("Text message: [{}]", text)
  }

}

