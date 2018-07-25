package org.blockstack.android.sdk

import org.json.JSONObject

/**
 * An object to configure options for `encrypt` and `decrypt` operations.
 *
 * @property publicKey the hex string of the ECDSA public key to use for decryption.
 */
class CryptoOptions(val publicKey: String? = null, val privateKey: String? = null) {

    fun toJSON(): JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("privateKey", if (privateKey == null) JSONObject.NULL else privateKey)
        optionsObject.put("publicKey", if (publicKey == null) JSONObject.NULL else publicKey)
        return optionsObject
    }

    override fun toString(): String {
        return toJSON().toString()
    }
}
