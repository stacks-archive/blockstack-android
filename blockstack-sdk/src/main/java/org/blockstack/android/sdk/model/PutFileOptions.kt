package org.blockstack.android.sdk.model

import org.json.JSONObject

/**
 * An object to configure options for `putFile` operations.
 *
 * @property encrypt encrypt the with the private key of the current user before writing to storage
 * @property contentType contentType of file to be used only if not encrypted
 * @property sign Sign the data using ECDSA on SHA256 hashes with the user's app private key. If a string is specified, it is used as the private key.
 */
public class PutFileOptions(val encrypt: Boolean = true, val contentType: String? = null, val sign: Boolean = false) {

    /**
     * json representation of these options as used by blockstack.js
     */
    fun toJSON(): JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("encrypt", encrypt)
        if (!encrypt && contentType != null) {
            optionsObject.put("contentType", contentType)
        }
        optionsObject.put("sign", sign)
        return optionsObject
    }

    /**
     * string representation in json format used by blockstack.js
     */
    override fun toString(): String {
        return toJSON().toString()
    }

}