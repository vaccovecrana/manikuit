package io.vacco.mk.config

data class CoinbaseConfig(
    var exchangeRates: String = "exchange-rates"
): HttpConfig()