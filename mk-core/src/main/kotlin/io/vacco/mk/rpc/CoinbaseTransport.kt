package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.config.CoinbaseConfig

class CoinbaseTransport(private val config: CoinbaseConfig) : MkCachingTransport(config) {

  var currentRates: Collection<MkExchangeRate> = ArrayList()

  fun getExchangeRateFor(type: MkAccount.Crypto,
                         fiatCurrency: String): MkExchangeRate? {
    requireNotNull(type) // TODO optimize this with caching.
    requireNotNull(fiatCurrency)
    return currentRates
        .filter { ex0 -> ex0.crypto == type }
        .firstOrNull { ex0 -> ex0.fiat == fiatCurrency }
  }

  override fun update() {
    currentRates = MkAccount.Crypto.values()
        .filter { r0 -> r0 != MkAccount.Crypto.UNKNOWN }
        .flatMap { r0 -> load(r0).asIterable() }
  }

  override fun purge() { currentRates = ArrayList() }

  private fun load(cc0: MkAccount.Crypto): Sequence<MkExchangeRate> {
    requireNotNull(cc0)
    val json = getJson(config.exchangeRates, Pair("currency", cc0))
    val root = mapper.readTree(json)
    return root.path("data").path("rates").fields().asSequence().map { n0 ->
      MkExchangeRate()
          .withCrypto(cc0)
          .withFiat(n0.key)
          .withLast(n0.value.asDouble())
    }
  }
}
