package io.vacco.mk.rpc

import io.vacco.mk.base.HttpConfig
import javafx.util.Pair
import okhttp3.*
import org.slf4j.*

typealias QueryParam = Pair<String, Any>

open class HttpTransport(config: HttpConfig) {

  private val log: Logger = LoggerFactory.getLogger(this.javaClass)
  private val rootUrl = HttpUrl.parse(config.rootUrl)
  private var client: OkHttpClient

  init {
    var bld = OkHttpClient.Builder()
    if (config.username != null) {
      if (config.password != null) {
        bld.authenticator { _, response ->
          val credential = Credentials.basic(config.username, config.password)
          response.request().newBuilder().header("Authorization", credential).build()
        }
      }
    }
    if (config.isIgnoreSsl) { bld = bld.hostnameVerifier { _, _ -> true } }
    this.client = bld.build()
  }

  fun getJson(path: String, vararg params: QueryParam): String {
    return processJson(Request.Builder().url(resolve(path, *params)).get().build())
  }

  fun postJson(path: String?, jsonPayload: String): String {
    if (log.isTraceEnabled) { log.trace("RPC POST payload: [$jsonPayload]") }
    return processJson(Request.Builder()
        .url(resolve(path))
        .post(RequestBody.create(MediaType.parse("application/json"), jsonPayload))
        .build())
  }

  fun postJson(jsonPayload: String): String { return postJson(null, jsonPayload) }

  private fun processJson(r0: Request): String {
    if (log.isTraceEnabled) { log.trace("Raw HTTP request: [$r0]") }
    client.newCall(r0).execute().use { response ->
      if (response.isSuccessful) {
        val json = response.body()!!.string()
        log.trace("Response: [$json]")
        return json
      }
      val errorMsg = "[${response.code()}] -> ${response.message()}"
      log.error(errorMsg)
      throw IllegalStateException(errorMsg)
    }
  }

  private fun resolve(path: String?, vararg parameters: QueryParam): HttpUrl {
    val b0 = rootUrl!!.newBuilder()
    if (path != null) { b0.addPathSegments(path) }
    parameters.forEach { p -> b0.addQueryParameter(p.key, p.value.toString()) }
    return b0.build()
  }
}