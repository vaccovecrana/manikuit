package io.vacco.mk.config

data class BitcoinFeesConfig(
    var recommended: String = "api/v1/fees/recommended"
): HttpConfig()