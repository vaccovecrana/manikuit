package unit

import io.vacco.mk.rpc.CoinbaseTransport
import io.vacco.mk.base.*
import io.vacco.mk.config.BitcoinFeesConfig
import io.vacco.mk.config.CoinbaseConfig
import io.vacco.mk.rpc.BitcoinFeesTransport
import j8spec.J8Spec.beforeAll
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.it
import org.junit.Assert.*

@DefinedOrder
@RunWith(J8SpecRunner::class)
class RpcTransportSpec {

  private var transport: CoinbaseTransport? = null
  private var bft: BitcoinFeesTransport? = null

  init {
    beforeAll {
      val source = CoinbaseConfig()
      source.rootUrl = "https://api.coinbase.com/v2/"
      transport = CoinbaseTransport(source)
      val bfc = BitcoinFeesConfig()
      bfc.rootUrl = "https://bitcoinfees.earn.com"
      bft = BitcoinFeesTransport(bfc)
    }
    it("Can build an exchange rate collection for supported crypto currencies.") {
      transport!!.update()
      assertFalse(transport!!.currentRates.isEmpty())
    }
    it("Can load exchange rate for BTC/USD.") {
      val xr = transport!!.getExchangeRateFor(MkExchangeRate.Crypto.BTC, "USD")
      assertNotNull(xr)
    }
    it("Can load the latest recommended BTC transaction fees.") {
      bft!!.update()
      assertNotNull(bft!!.hourFee)
      assertNotNull(bft!!.halfHourFee)
      assertNotNull(bft!!.fastestFee)
    }
  }
}
