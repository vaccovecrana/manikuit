package io.vacco.mk.config

open class BitcoinFeesConfig(
    var recommended: String = "api/v1/fees/recommended"
): HttpConfig()