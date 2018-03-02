package unit

import io.vacco.mk.util.SecretUtils
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import org.junit.runner.RunWith

import j8spec.J8Spec.it
import org.junit.Assert.*

@DefinedOrder
@RunWith(J8SpecRunner::class)
class SecretUtilsSpec {

  private var parts: Collection<String>? = null
  private val sec = "I am the great gopher."

  init {
    it("Can split a secret in 3 parts, with 2 required.") {
      parts = SecretUtils.split(sec, 3, 2)
      assertFalse(parts!!.isEmpty())
      assertTrue(parts!!.size == 3)
    }
    it("Can join a secret using 2 out of 3 parts.") {
      val partArray = parts!!.toTypedArray()
      assertEquals(SecretUtils.join(partArray[0], partArray[2]), sec)
    }
    it("Cannot split a secret with zero parts.", { c -> c.expected(IllegalArgumentException::class.java) }
    ) { SecretUtils.split(sec, 0, 2) }
    it("Cannot split a secret with zero required parts.", { c -> c.expected(IllegalArgumentException::class.java) }
    ) { SecretUtils.split(sec, 3, 0) }
    it("Cannot split a secret with more required than total parts.", { c -> c.expected(IllegalArgumentException::class.java) }
    ) { SecretUtils.split(sec, 3, 4) }
    it("Cannot join a secret with no parts.", { c -> c.expected(IllegalArgumentException::class.java) })
    { SecretUtils.join() }
  }
}
