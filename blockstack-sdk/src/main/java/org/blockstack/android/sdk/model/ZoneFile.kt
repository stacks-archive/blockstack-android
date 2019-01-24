package org.blockstack.android.sdk.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Object representing a zone file
 */
class ZoneFile(private val jsonObject: JSONObject) {

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() = jsonObject


    /**
     * The target of the first uri entry or null if not defined
     */
    val tokenFileUri: String?
        get() {
            if (!jsonObject.has("uri")) {
                return null
            }
            val uris: JSONArray? = jsonObject.optJSONArray("uri")
            if (uris == null) {
                return null
            }
            if (uris.length() < 1) {
                return null
            }
            val firstUriRecord = uris.getJSONObject(0)

            if (!firstUriRecord.has("target")) {
                return null
            }
            var tokenFileUrl = firstUriRecord.getString("target")

            if (tokenFileUrl.startsWith("https")) {
                // pass
            } else if (tokenFileUrl.startsWith("http")) {
                // pass
            } else {
                tokenFileUrl = "https://${tokenFileUrl}"
            }
            return tokenFileUrl
        }
}