package org.blockstack.android.sdk

import org.json.JSONObject
import java.net.URL

/**
 * An object to configure options for `getFile` operations.
 *
 * @property decrypt attempt to decrypt the file
 * @property username the username of the user from which you wish to read the file
 * @property app the app from which to read the file
 * @property zoneFileLookupURL The URL to use for zonefile lookup.
 * If `null`, this will use a default lookup URL.
 */
public class GetFileOptions(val decrypt: Boolean = true,
                            val username: String? = null,
                            val app: String? = null,
                            val zoneFileLookupURL: URL? = null) {

    fun toJSON() : JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("decrypt", decrypt)
        optionsObject.put("username", if(username.isNullOrBlank()) JSONObject.NULL else username)
        optionsObject.put("app", if(app.isNullOrBlank()) JSONObject.NULL else app)
        optionsObject.put("zoneFileLookupURL", if(zoneFileLookupURL == null) JSONObject.NULL else zoneFileLookupURL)
        return optionsObject
    }

    override fun toString(): String {
        return toJSON().toString()
    }

}