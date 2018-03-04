package io.vacco.mk.config

import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

data class MkConfig(
    var confirmationThreshold: Long = 0,
    var blockScanLimit: Long = 0,
    var blockScanLimitUnit: ChronoUnit = ChronoUnit.FOREVER,
    var blockCacheLimit: Long = 0,
    var blockCacheLimitUnit: TimeUnit = TimeUnit.NANOSECONDS
): HttpConfig()