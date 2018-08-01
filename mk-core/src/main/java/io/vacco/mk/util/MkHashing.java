package io.vacco.mk.util;

import net.openhft.hashing.LongHashFunction;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class MkHashing {

  public static String withSeed(long seed, Object ... args) {
    List<byte[]> bytes = Arrays.stream(args)
        .filter(Objects::nonNull)
        .map(Object::toString)
        .map(String::getBytes)
        .collect(Collectors.toList());
    ByteBuffer bb = ByteBuffer.allocateDirect(bytes.stream().mapToInt(ba -> ba.length).sum());
    bytes.forEach(bb::put);
    long hash = LongHashFunction.xx(seed).hashBytes(bb);
    return asHex(longToBytes(hash));
  }

  public static String apply(Object... args) {
    return withSeed(0, args);
  }

  private static String asHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(64);
    for (int i = 0; i < bytes.length; i++) {
      sb.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16));
      sb.append(Character.forDigit((bytes[i] & 0xF), 16));
    }
    return sb.toString();
  }

  private static byte[] longToBytes(long l) {
    byte[] result = new byte[8];
    for (int i = 7; i >= 0; i--) {
      result[i] = (byte) (l & 0xFF);
      l >>= 8;
    }
    return result;
  }
}
