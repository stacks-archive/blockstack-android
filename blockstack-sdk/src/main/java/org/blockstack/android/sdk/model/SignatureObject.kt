package org.blockstack.android.sdk.model

import okio.ByteString

data class SignatureObject(val signature: String, val publicKey: String) {
    fun toJSONString(): ByteString {
        val jsonString = "{signature:\"$signature\",publicKey:\"$publicKey\"}"
        val bytes = jsonString.toByteArray()
        return ByteString.of(bytes, 0, bytes.size)
    }
}
