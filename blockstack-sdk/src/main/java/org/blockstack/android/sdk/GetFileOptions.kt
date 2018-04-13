package org.blockstack.android.sdk

import org.json.JSONObject
import java.net.URL

public class GetFileOptions(val decrypt: Boolean = true,
                            val zoneFileLookupURL: URL? = null) {

    fun toJSON() : JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("decrypt", decrypt)
        optionsObject.put("zoneFileLookupURL", if(zoneFileLookupURL == null) JSONObject.NULL else zoneFileLookupURL)
        return optionsObject
    }

    override fun toString(): String {
        return toJSON().toString()
    }

}