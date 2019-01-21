package org.blockstack.android.sdk.model

import org.json.JSONObject

/**
 * Object containing an encoded and corresponding decoded profile token
 */
class ProfileTokenPair(private val jsonObject: JSONObject) {

    /**
     * The encoded token.
     */
    val token: String? = jsonObject.optString("token")

    /**
     * The decoded token.
     */
    val decodedToken: ProfileToken? = if (jsonObject.has("decodedToken")) {
        ProfileToken(jsonObject.getJSONObject("decodedToken"))
    } else {
        null
    }

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() = jsonObject
}