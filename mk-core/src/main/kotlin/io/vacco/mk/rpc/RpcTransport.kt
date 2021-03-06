package io.vacco.mk.rpc

import com.fasterxml.jackson.databind.*
import io.vacco.mk.base.rpc.RpcRequest
import io.vacco.mk.base.rpc.RpcResponse
import io.vacco.mk.config.HttpConfig

import java.io.IOException
import java.util.Arrays
import java.util.UUID

open class RpcTransport(config: HttpConfig) : HttpTransport(config) {

  protected val mapper = ObjectMapper()

  protected fun <T> rpcRequest(target: Class<T>, method: String, vararg params: Any): Pair<RpcResponse, T> {
    var r0 = RpcRequest(id = UUID.randomUUID().toString(), method = requireNotNull(method))
    if (params.isNotEmpty()) { r0 = r0.copy(params = Arrays.asList(*params)) }
    val rJson = mapper.writeValueAsString(r0)
    val rs0 = mapper.readValue(postJson(rJson), RpcResponse::class.java)
    if (rs0.error != null) { throw IOException(rs0.error!!.message) }
    return getResult(rs0, target)
  }

  private fun <T> getResult(response: RpcResponse, target: Class<T>): Pair<RpcResponse, T> {
    requireNotNull(response)
    var rawResult = response.result
    if (target == JsonNode::class.java) { rawResult = mapper.valueToTree(rawResult) }
    return Pair(response, mapper.convertValue(rawResult, target))
  }
}
