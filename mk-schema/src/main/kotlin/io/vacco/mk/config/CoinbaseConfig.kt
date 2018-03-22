package io.vacco.mk.config

open class CoinbaseConfig(
    var exchangeRates: String = "exchange-rates"
): HttpConfig()