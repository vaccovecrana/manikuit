package unit

import io.vacco.mk.base.MkBlock
import io.vacco.mk.base.MkExchangeRate
import io.vacco.mk.spi.MkBlockCache
import io.vacco.mk.util.NoOpBlockCache
import j8spec.J8Spec.*
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith

@DefinedOrder
@RunWith(J8SpecRunner::class)
class NoOpBlockCacheSpec {

  var cache: MkBlockCache = NoOpBlockCache()

  init {
    it("Performs storage no-ops.") {
      cache.purge(9999, MkExchangeRate.Crypto.BTC)
      cache.storeBlock(MkBlock(
          id = "lol", height = 999, timeUtcSec = 1523719651,
          hash = "0xlol", type = MkExchangeRate.Crypto.BTC
      ))
      cache.storeRecords(emptyList())
      cache.getLatestLocalBlockFor(MkExchangeRate.Crypto.BTC)
      cache.getPaymentsFor("LOL", MkExchangeRate.Crypto.BTC)
    }
  }

}