package io.vacco.mk.util

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class Ciphertext(val ciphertext: ByteArray, val iv: ByteArray)

object GcmCrypto {

  private val secureRandom = SecureRandom()

  /**
   * Generates a key with [sizeInBits] bits.
   */
  fun generateKey(sizeInBits: Int): ByteArray {
    val result = ByteArray(sizeInBits / 8)
    secureRandom.nextBytes(result)
    return result
  }

  /**
   * Generates a nonce for GCM mode. The nonce is always 96 bit long.
   */
  private fun generateNonce(): ByteArray {
    val result = ByteArray(96 / 8)
    secureRandom.nextBytes(result)
    return result
  }

  /**
   * Encrypts the given [plaintext] with the given [key] under AES GCM.
   *
   * This method generates a random nonce.
   *
   * @return Ciphertext and nonce
   */
  fun encryptGcm(plaintext: ByteArray, key: ByteArray): Ciphertext {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val keySpec = SecretKeySpec(key, "AES")
    val nonce = generateNonce()
    val gcmSpec = GCMParameterSpec(128, nonce) // 128 bit authentication tag
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
    val ciphertext = cipher.doFinal(plaintext)
    return Ciphertext(ciphertext, nonce)
  }

  /**
   * Decrypts the given [ciphertext] using the given [key] under AES GCM.
   *
   * @return Plaintext
   */
  fun decryptGcm(ciphertext: Ciphertext, key: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val keySpec = SecretKeySpec(key, "AES")
    val gcmSpec = GCMParameterSpec(128, ciphertext.iv) // 128 bit authentication tag
    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
    return cipher.doFinal(ciphertext.ciphertext)
  }
}
