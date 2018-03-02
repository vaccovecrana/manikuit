package io.vacco.mk.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public final class MurmurHash3 {

  private static int seed = new Random().nextInt();

  /**
   * 128 bits of state
   */
  public static final class LongPair {
    public long val1;
    public long val2;
  }

  public static String apply(Object ... items) {
    Optional<byte []> oraw = Arrays.stream(items)
        .filter(Objects::nonNull)
        .map(Object::toString)
        .map(String::getBytes)
        .reduce((ba0, ba1) -> {
          byte [] ba = new byte[ba0.length + ba1.length];
          System.arraycopy(ba0, 0, ba, 0, ba0.length);
          System.arraycopy(ba1, 0, ba, ba0.length, ba1.length);
          return ba;
        });
    if (oraw.isPresent()) {
      byte [] raw = oraw.get();
      LongPair l0 = new LongPair();
      MurmurHash3.murmurhash3_x64_128(raw, 0, raw.length, seed, l0);
      return String.format("%s-%s", Long.toHexString(l0.val1), Long.toHexString(l0.val2));
    }
    return null;
  }

  public static final long fmix64(long k) {
    k ^= k >>> 33;
    k *= 0xff51afd7ed558ccdL;
    k ^= k >>> 33;
    k *= 0xc4ceb9fe1a85ec53L;
    k ^= k >>> 33;
    return k;
  }

  /**
   * Gets a long from a byte buffer in little endian byte order.
   */
  public static final long getLongLittleEndian(byte[] buf, int offset) {
    return ((long) buf[offset + 7] << 56)   // no mask needed
        | ((buf[offset + 6] & 0xffL) << 48)
        | ((buf[offset + 5] & 0xffL) << 40)
        | ((buf[offset + 4] & 0xffL) << 32)
        | ((buf[offset + 3] & 0xffL) << 24)
        | ((buf[offset + 2] & 0xffL) << 16)
        | ((buf[offset + 1] & 0xffL) << 8)
        | ((buf[offset] & 0xffL));        // no shift needed
  }

  /**
   * Returns the MurmurHash3_x64_128 hash, placing the result in "out".
   */
  public static void murmurhash3_x64_128(byte[] key, int offset, int len, int seed, LongPair out) {
    // The original algorithm does have a 32 bit unsigned seed.
    // We have to mask to match the behavior of the unsigned types and prevent sign extension.
    long h1 = seed & 0x00000000FFFFFFFFL;
    long h2 = seed & 0x00000000FFFFFFFFL;

    final long c1 = 0x87c37b91114253d5L;
    final long c2 = 0x4cf5ad432745937fL;

    int roundedEnd = offset + (len & 0xFFFFFFF0);  // round down to 16 byte block
    for (int i = offset; i < roundedEnd; i += 16) {
      long k1 = getLongLittleEndian(key, i);
      long k2 = getLongLittleEndian(key, i + 8);
      k1 *= c1;
      k1 = Long.rotateLeft(k1, 31);
      k1 *= c2;
      h1 ^= k1;
      h1 = Long.rotateLeft(h1, 27);
      h1 += h2;
      h1 = h1 * 5 + 0x52dce729;
      k2 *= c2;
      k2 = Long.rotateLeft(k2, 33);
      k2 *= c1;
      h2 ^= k2;
      h2 = Long.rotateLeft(h2, 31);
      h2 += h1;
      h2 = h2 * 5 + 0x38495ab5;
    }

    long k1 = 0;
    long k2 = 0;

    switch (len & 15) {
      case 15:
        k2 = (key[roundedEnd + 14] & 0xffL) << 48;
      case 14:
        k2 |= (key[roundedEnd + 13] & 0xffL) << 40;
      case 13:
        k2 |= (key[roundedEnd + 12] & 0xffL) << 32;
      case 12:
        k2 |= (key[roundedEnd + 11] & 0xffL) << 24;
      case 11:
        k2 |= (key[roundedEnd + 10] & 0xffL) << 16;
      case 10:
        k2 |= (key[roundedEnd + 9] & 0xffL) << 8;
      case 9:
        k2 |= (key[roundedEnd + 8] & 0xffL);
        k2 *= c2;
        k2 = Long.rotateLeft(k2, 33);
        k2 *= c1;
        h2 ^= k2;
      case 8:
        k1 = ((long) key[roundedEnd + 7]) << 56;
      case 7:
        k1 |= (key[roundedEnd + 6] & 0xffL) << 48;
      case 6:
        k1 |= (key[roundedEnd + 5] & 0xffL) << 40;
      case 5:
        k1 |= (key[roundedEnd + 4] & 0xffL) << 32;
      case 4:
        k1 |= (key[roundedEnd + 3] & 0xffL) << 24;
      case 3:
        k1 |= (key[roundedEnd + 2] & 0xffL) << 16;
      case 2:
        k1 |= (key[roundedEnd + 1] & 0xffL) << 8;
      case 1:
        k1 |= (key[roundedEnd] & 0xffL);
        k1 *= c1;
        k1 = Long.rotateLeft(k1, 31);
        k1 *= c2;
        h1 ^= k1;
    }

    //----------
    // finalization

    h1 ^= len;
    h2 ^= len;

    h1 += h2;
    h2 += h1;

    h1 = fmix64(h1);
    h2 = fmix64(h2);

    h1 += h2;
    h2 += h1;

    out.val1 = h1;
    out.val2 = h2;
  }

}
