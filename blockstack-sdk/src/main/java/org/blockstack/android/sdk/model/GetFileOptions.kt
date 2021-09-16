package org.blockstack.android.sdk.model

import org.json.JSONObject
import java.net.URL

/**
 * An object to configure options for `getFile` operations.
 *
 * @property decrypt attempt to decrypt the file
 * @property verify indicate if the content should be verified. Verification also requires that
 * UserSession.putFile was set to sign=true.
 * @property username the username of the user from which you wish to read the file
 * @property app the app from which to read the file
 * @property zoneFileLookupURL The URL to use for zonefile lookup.
 * If `null`, this will use a default lookup URL.
 */
public class GetFileOptions(val decrypt: Boolean = true,
                            val verify: Boolean = false,
                            val username: String? = null,
                            val app: String? = null,
                            val zoneFileLookupURL: URL? = null,
                            val dir: String = "") {

    /**
     * json representation of these options as used by blockstack.js
     */
    fun toJSON(): JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("decrypt", decrypt)
        optionsObject.put("verify", verify)
        optionsObject.put("username", if (username.isNullOrBlank()) JSONObject.NULL else username)
        optionsObject.put("app", if (app.isNullOrBlank()) JSONObject.NULL else app)
        optionsObject.put("zoneFileLookupURL", if (zoneFileLookupURL == null) JSONObject.NULL else zoneFileLookupURL)
        return optionsObject
    }

    /**
     * string representation in json format used by blockstack.js
     */
    override fun toString(): String {
        return toJSON().toString()
    }

}