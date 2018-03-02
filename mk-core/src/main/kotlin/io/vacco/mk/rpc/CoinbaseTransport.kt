package io.vacco.mk.rpc

import io.vacco.mk.base.*
import javafx.util.Pair

class CoinbaseTransport(private val config: CoinbaseConfig) : RpcTransport(config) {

  fun getExchangeRateFor(type: MkExchangeRate.CryptoCurrency,
                         fiatCurrency: String): MkExchangeRate? {
    requireNotNull(type) // TODO optimize this with caching.
    requireNotNull(fiatCurrency)
    return load(type)
        .filter { ex0 -> ex0.fiatCurrency == fiatCurrency }
        .firstOrNull()
  }

  fun buildRates(): Collection<MkExchangeRate> {
    return MkExchangeRate.CryptoCurrency.values()
        .filter { r0 -> r0 != MkExchangeRate.CryptoCurrency.UNKNOWN }
        .flatMap { r0 -> load(r0).asIterable() }
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
