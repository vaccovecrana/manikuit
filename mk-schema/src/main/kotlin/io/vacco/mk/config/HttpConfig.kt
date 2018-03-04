package io.vacco.mk.config
open class HttpConfig(
    var rootUrl: String = "",
    var username: String = "",
    var password: String = "",
    var ignoreSsl: Boolean = false
)