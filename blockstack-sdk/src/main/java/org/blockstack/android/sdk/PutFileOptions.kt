package org.blockstack.android.sdk

import org.json.JSONObject
import java.net.URL

/**
 * An object to configure options for `putFile` operations.
 *
 * @property encrypt encrypt the with the private key of the current user before writing to storage
 */
public class PutFileOptions(val encrypt: Boolean = true) {

    /**
     * json representation of these options as used by blockstack.js
     */
    fun toJSON() : JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("encrypt", encrypt)
        return optionsObject
    }

    /**
     * string representation in json format used by blockstack.js
     */
    override fun toString(): String {
        return toJSON().toString()
    }

}