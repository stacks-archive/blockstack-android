package org.blockstack.android.sdk

import org.json.JSONObject
import java.net.URL

/**
 * An object to configure options for `putFile` operations.
 *
 * @property encrypt encrypt the with the private key of the current user before writing to storage
 */
public class PutFileOptions(val encrypt: Boolean = true) {

    fun toJSON() : JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("encrypt", encrypt)
        return optionsObject
    }

    override fun toString(): String {
        return toJSON().toString()
    }

}