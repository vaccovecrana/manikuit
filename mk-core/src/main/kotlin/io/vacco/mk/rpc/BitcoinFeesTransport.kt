package io.vacco.mk.rpc

import io.vacco.mk.config.BitcoinFeesConfig

class BitcoinFeesTransport(private val config: BitcoinFeesConfig): MkCachingTransport(config) {

  var fastestFee: Long = -1
  var halfHourFee: Long = -1
  var hourFee: Long = -1

  override fun update() {
    val json = getJson(config.recommended)
    val root = mapper.readTree(json)
    fastestFee = root.path("fastestFee").asLong()
    halfHourFee = root.path("halfHourFee").asLong()
    hourFee = root.path("hourFee").asLong()
  }

  override fun purge() {
    fastestFee = -1
    halfHourFee = -1
    hourFee = -1
  }
}