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
    it("Checks that different hashed object sequences have different hashed values") {
      val hash0 = MkHashing.apply(mbiapn, "lol")
      val hash1 = MkHashing.apply(mbiapn, "84756")
      val hash2 = MkHashing.apply(null, null, 9, 0)
      require(hash0 != hash1)
      require(hash1 != hash2)
      require(hash0 != hash2)
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
