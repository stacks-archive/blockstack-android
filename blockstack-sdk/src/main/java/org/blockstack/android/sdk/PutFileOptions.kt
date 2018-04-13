package org.blockstack.android.sdk

import org.json.JSONObject
import java.net.URL


public class PutFileOptions(val encrypt: Boolean = true,
                               val username: String? = null,
                               val app: String? = null,
                               val zoneFileLookupURL: URL? = null) {

    fun toJSON() : JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("encrypt", encrypt)
        optionsObject.put("username", if(username.isNullOrBlank()) JSONObject.NULL else username)
        optionsObject.put("app", if(app.isNullOrBlank()) JSONObject.NULL else app)
        optionsObject.put("zoneFileLookupURL", if(zoneFileLookupURL == null) JSONObject.NULL else zoneFileLookupURL)
        return optionsObject
    }

    override fun toString(): String {
        return toJSON().toString()
    }

}