package io.vacco.mk.util

import org.mitre.secretsharing.Part
import org.mitre.secretsharing.Secrets
import java.security.SecureRandom

object SecretUtils {

  private val r = SecureRandom()

  fun split(secret: String, total: Int, required: Int): Set<String> {
    requireNotNull(secret)
    require(total > 0)
    require(required > 0)
    require(required <= total)
    return Secrets.split(secret.toByteArray(), total, required, r)
        .map(Part::toString).toHashSet()
  }

  fun join(vararg parts: String): String {
    require(parts.isNotEmpty())
    return String(Secrets.join(parts.map { partStr -> Part(partStr) }.toTypedArray()))
  }
}
