package io.vacco.mk.rpc

import io.vacco.mk.config.HttpConfig
import okhttp3.*
import org.slf4j.*
import java.util.concurrent.ForkJoinPool

typealias QueryParam = Pair<String, Any>

open class HttpTransport(config: HttpConfig) {

  private val log: Logger = LoggerFactory.getLogger(this.javaClass)
  private val rootUrl = HttpUrl.parse(config.rootUrl)
  protected var client: OkHttpClient

  init {
    var bld = OkHttpClient.Builder().dispatcher(Dispatcher(ForkJoinPool.commonPool()))
    if (config.connectionPoolSize != -1) {
      val connPool = ConnectionPool(config.connectionPoolSize, config.keepAlive, config.keepAliveUnit)
      bld = bld.connectionPool(connPool)
    }
    if (config.username.isNotEmpty()) {
      if (config.password.isNotEmpty()) {
        bld.authenticator { _, response ->
          val credential = Credentials.basic(config.username, config.password)
          response.request().newBuilder().header("Authorization", credential).build()
        }
      }
    }
    if (config.ignoreSsl) { bld = bld.hostnameVerifier { _, _ -> true } }
    this.client = bld.build()
  }

  fun getJson(path: String, vararg params: QueryParam): String {
    return processJson(Request.Builder().url(resolve(path, *params)).get().build())
  }

  fun postJson(path: String?, jsonPayload: String, headers: Map<String, String>?, vararg params: QueryParam): String {
    if (log.isDebugEnabled) {
      log.debug("RPC POST: [$jsonPayload]")
      if (headers != null) { log.debug("Headers: [$headers]") }
    }
    var builder = Request.Builder()
        .url(resolve(path, *params))
        .post(RequestBody.create(MediaType.parse("application/json"), jsonPayload))
    if (headers != null && headers.isNotEmpty()) {
      builder = builder.headers(Headers.of(headers))
    }
    return processJson(builder.build())
  }

  fun postJson(path: String?, jsonPayload: String): String = postJson(path, jsonPayload, null)

  fun postJson(jsonPayload: String): String = postJson(null, jsonPayload)

  private fun processJson(r0: Request): String {
    if (log.isTraceEnabled) { log.trace("Raw HTTP request: [$r0]") }
    client.newCall(r0).execute().use { response ->
      if (response.isSuccessful) {
        val json = response.body()!!.string()
        if (log.isDebugEnabled) { log.debug("Response: [$json]") }
        return json
      }
      val errorMsg = "[${response.code()}] -> ${response.message()}"
      if (log.isDebugEnabled) {
        log.debug(errorMsg)
        val errorBody = response.body()
        if (errorBody != null) { log.debug(errorBody.string()) }
      }
      throw IllegalStateException(errorMsg)
    }
  }

  private fun resolve(path: String?, vararg parameters: QueryParam): HttpUrl {
    val b0 = rootUrl!!.newBuilder()
    if (path != null) { b0.addPathSegments(path) }
    parameters.forEach { p -> b0.addQueryParameter(p.first, p.second.toString()) }
    return b0.build()
  }
}