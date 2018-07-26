package org.blockstack.android.sdk

import org.json.JSONObject

/**
 * An object to configure options for `encrypt` and `decrypt` operations.
 *
 * @property privateKey the hex string of the ECDSA private key to use for decryption.
 */
class CryptoOptions(val privateKey: String?) {

    /**
     * json representation of these options
     */
    fun toJSON(): JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("privateKey", if (privateKey == null) JSONObject.NULL else privateKey)
        return optionsObject
    }

    /**
     * string representation in json format used by blockstack.js
     */
    override fun toString(): String {
        return toJSON().toString()
    }
}
