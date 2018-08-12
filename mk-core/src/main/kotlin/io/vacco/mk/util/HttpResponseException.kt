package io.vacco.mk.util

class HttpResponseException(val statusCode: Int, override var message: String? = null,
                            var errorBody: String? = null) : IllegalStateException(message) {
  override fun toString(): String = "[$statusCode -> $message : $errorBody]"
}
