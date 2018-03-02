package unit

import io.vacco.mk.rpc.CoinbaseTransport
import io.vacco.mk.base.*
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith
import j8spec.J8Spec.it
import org.junit.Assert.*

@DefinedOrder
@RunWith(J8SpecRunner::class)
class RpcTransportSpec {

  private val source = CoinbaseConfig()
      .withRootUrl("https://api.coinbase.com/v2/")
      .withIgnoreSsl(false)
  private val transport = CoinbaseTransport(source)

  init {
    it("Can build an exchange rate collection for supported crypto currencies.") {
      val rates = transport.buildRates()
      assertFalse(rates.isEmpty())
    }
    it("Can load exchange rate for BTC/USD.") {
      val xr = transport.getExchangeRateFor(MkExchangeRate.CryptoCurrency.BTC, "USD")
      assertNotNull(xr)
    }
  }
}
