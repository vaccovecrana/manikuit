package com.ifesdjeen.blomstre;

/*
 * !!! THIS SOURCE FILE WAS ORIGINALLY TAKEN FROM APACHE CASSANDRA SOURCE !!!
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.openhft.hashing.LongHashFunction;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class BloomFilter<T> {

  private static final long BITSET_EXCESS = 20;

  private final Function<T, ByteBuffer> converter;
  private final ConcurrentBitSet bitset;
  private final int hashCount;

  private BloomFilter(Function<T, ByteBuffer> converter,
              int hashes,
              ConcurrentBitSet bitset
  ) {
    this.converter = converter;
    this.hashCount = hashes;
    this.bitset = bitset;
  }

  private long[] getHashBuckets(ByteBuffer key) {
    return getHashBuckets(key, hashCount, bitset.capacity());
  }

  private long[] getHashBuckets(ByteBuffer b, int hashCount, long max) {
    final long[] result = new long[hashCount];
    b.clear();
    final long xxH = LongHashFunction.xx().hashBytes(b);
    b.clear();
    final long m3 = LongHashFunction.murmur_3().hashBytes(b);
    final long [] hash = new long [] {xxH, m3};
    for (int i = 0; i < hashCount; ++i) {
      result[i] = Math.abs((hash[0] + (long) i * hash[1]) % max);
    }
    return result;
  }

  public void add(T key) {
    add(converter.apply(key));
  }

  private void add(ByteBuffer key) {
    for (long bucketIndex : getHashBuckets(key)) {
      bitset.set(bucketIndex);
    }
  }

  public boolean isPresent(T key) {
    return isPresent(converter.apply(key));
  }

  private boolean isPresent(ByteBuffer key) {
    for (long bucketIndex : getHashBuckets(key)) {
      if (!bitset.get(bucketIndex)) {
        return false;
      }
    }
    return true;
  }

  public void clear() {
    bitset.clear();
  }

  public static <T> BloomFilter<T> makeFilter(Function<T, ByteBuffer> converter,
                                              int numElements,
                                              double maxFalsePosProbability) {
    int maxBucketsPerElement = BloomCalculations.maxBucketsPerElement(numElements);
    BloomCalculations.BloomSpecification spec =
        BloomCalculations.computeBloomSpec(maxBucketsPerElement, maxFalsePosProbability);
    long numBits = (numElements * spec.bucketsPerElement) + BITSET_EXCESS;
    return new BloomFilter<>(converter, spec.K, new ConcurrentBitSet(numBits));
  }
}
