package org.blockstack.android.sdk.model

import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject

data class SignatureObject(val signature: String, val publicKey: String) {
    fun toJSONByteString(): ByteString {
        val jsonString = JSONObject()
                .put("signature", signature)
                .put("publicKey", publicKey).toString()
        val bytes = jsonString.toByteArray()
        return bytes.toByteString(0, bytes.size)
    }

    companion object {
        fun fromJSONString(jsonString: String): SignatureObject {
            val json = JSONObject(jsonString)
            return SignatureObject(json.getString("signature"), json.getString("publicKey"))
        }
    }
}

data class SignedCipherObject(val signature: String, val publicKey: String, val cipherText: String) {
    fun toJSONByteString(): ByteString {
        val jsonString = JSONObject()
                .put("signature", signature)
                .put("publicKey", publicKey)
                .put("cipherText", cipherText).toString()
        val bytes = jsonString.toByteArray()
        return bytes.toByteString(0, bytes.size)
    }

    companion object {
        fun fromJSONString(jsonString: String): SignedCipherObject {
            val json = JSONObject(jsonString)
            return SignedCipherObject(json.getString("signature"), json.getString("publicKey"), json.getString("cipherText"))
        }
    }
}
