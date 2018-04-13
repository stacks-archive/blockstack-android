package org.blockstack.android.sdk

import org.json.JSONObject
import java.net.URL


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