package io.vacco.mk.rpc

import io.vacco.mk.config.HttpConfig

abstract class MkCachingTransport(config: HttpConfig): RpcTransport(config) {

  abstract fun update()
  abstract fun purge()

}