package unit

import com.ifesdjeen.blomstre.BloomFilter
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
    it("Adds elements to the bloom filter") {
      bf.add(mbiapn)
    }
    it("Checks that an element is present in the bloom filter") {
      bf.add("0xe58230311c16fa171652f5b8fa569b95b4e5828f")
      require( bf.isPresent(mbiapn))
      require(!bf.isPresent("0xa5086cf7d153a06f2802e46d87bea16b1bd75f39"))
    }
  }
}
