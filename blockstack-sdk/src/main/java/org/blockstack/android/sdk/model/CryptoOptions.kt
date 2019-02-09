package org.blockstack.android.sdk.model

import org.json.JSONObject

/**
 * An object to configure options for `encrypt` and `decrypt` operations.
 *
 * @property publicKey the hex string of the ECDSA public key to use for encryption.
 * @property privateKey the hex string of the ECDSA private key to use for decryption.
 */
class CryptoOptions(val publicKey: String? = null, val privateKey: String? = null) {

    /**
     * json representation of these options
     */
    fun toJSON(): JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("privateKey", if (privateKey == null) JSONObject.NULL else privateKey)
        optionsObject.put("publicKey", if (publicKey == null) JSONObject.NULL else publicKey)
        return optionsObject
    }

    /**
     * string representation in json format used by blockstack.js
     */
    override fun toString(): String {
        return toJSON().toString()
    }
}
