package unit

import com.ifesdjeen.blomstre.BloomFilter
import io.vacco.mk.util.MkHashing
import j8spec.J8Spec.*
import j8spec.annotation.DefinedOrder
import j8spec.junit.J8SpecRunner
import net.openhft.hashing.LongHashFunction
import org.junit.runner.RunWith
import java.nio.ByteBuffer

@DefinedOrder
@RunWith(J8SpecRunner::class)
class BloomFilterSpec {

  init {
    val mbiapn = "My brother is a peanut head"
    val bf = BloomFilter.makeFilter<String>(
        { ByteBuffer.wrap(it.toByteArray()) } ,
        10000000, 0.001)

    it("Checks that a string has is the same at each run without custom seeding") {
      val hash = LongHashFunction.xx().hashChars(mbiapn)
      require(hash == 8223519531741484286)
      println(hash)
    }
    it("Can hash a series of values") {
      val hash = MkHashing.apply(mbiapn, null, 8, 999, 2.2, "test")
      require(hash.length == 16)
      println(hash)
    }
    it("Adds elements to the bloom filter") {
      bf.add(mbiapn)
    }
    it("Checks that an element is present in the bloom filter") {
      val p = bf.isPresent(mbiapn)
      require(p)
    }
  }
}
