package io.vacco.mk.rpc

import io.vacco.mk.base.*
import io.vacco.mk.config.CoinbaseConfig

class CoinbaseTransport(private val config: CoinbaseConfig) : RpcTransport(config) {

  var currentRates: Collection<MkExchangeRate> = ArrayList()

  fun getExchangeRateFor(type: MkExchangeRate.CryptoCurrency,
                         fiatCurrency: String): MkExchangeRate? {
    requireNotNull(type) // TODO optimize this with caching.
    requireNotNull(fiatCurrency)
    return currentRates
        .filter { ex0 -> ex0.cryptoCurrency == type }
        .firstOrNull { ex0 -> ex0.fiatCurrency == fiatCurrency }
  }

  fun update(): Collection<MkExchangeRate> {
    currentRates = MkExchangeRate.CryptoCurrency.values()
        .filter { r0 -> r0 != MkExchangeRate.CryptoCurrency.UNKNOWN }
        .flatMap { r0 -> load(r0).asIterable() }
    return currentRates
  }

  private fun load(cc0: MkExchangeRate.CryptoCurrency): Sequence<MkExchangeRate> {
    requireNotNull(cc0)
    val json = getJson(config.exchangeRates, Pair("currency", cc0))
    val root = mapper.readTree(json)
    return root.path("data").path("rates").fields().asSequence().map { n0 ->
      MkExchangeRate()
          .withCryptoCurrency(cc0)
          .withFiatCurrency(n0.key)
          .withLast(n0.value.asDouble())
    }
  }
}
