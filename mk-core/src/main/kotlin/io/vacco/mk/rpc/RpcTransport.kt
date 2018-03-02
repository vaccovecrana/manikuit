package io.vacco.mk.rpc

import com.fasterxml.jackson.databind.*
import io.vacco.mk.base.*
import javafx.util.Pair

import java.io.IOException
import java.util.Arrays
import java.util.UUID
import net.tribe7.reason.Check.notNull

open class RpcTransport(config: HttpConfig) : HttpTransport(config) {

  protected val mapper = ObjectMapper()

  protected fun <T> rpcRequest(target: Class<T>, method: String, vararg params: Any): Pair<RpcResponse, T> {
    try {
      var r0 = RpcRequest().withId(UUID.randomUUID().toString()).withMethod(notNull(method))
      if (params.isNotEmpty()) { r0 = r0.withParams(Arrays.asList(*params)) }
      val rJson = mapper.writeValueAsString(r0)
      val rs0 = mapper.readValue(postJson(rJson), RpcResponse::class.java)
      if (rs0.error != null) { throw IOException(rs0.error.message) }
      return getResult(rs0, target)
    } catch (e: Exception) { throw IllegalStateException(e) }
  }

  private fun <R : RpcResponse, T> getResult(response: R, target: Class<T>): Pair<R, T> {
    try {
      notNull(response)
      var rawResult = response.result
      if (target == JsonNode::class.java) { rawResult = mapper.valueToTree(rawResult) }
      return Pair(response, mapper.convertValue(rawResult, target))
    } catch (e: Exception) { throw IllegalStateException(e) }
  }
}
