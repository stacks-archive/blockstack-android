package org.blockstack.android.sdk.model

import org.json.JSONObject

/**
 * Object containing user data. This object is backed by the original JSON representation.
 *
 * This currently provides very minimal functionality. To access the full array of values,
 * you should parse the `json` property of this object or send a pull request to add additional
 * functionality to this class.
 */
class UserData(private val jsonObject: JSONObject) {


    /**
     * The profile of the user or null if not defined
     */
    val profile: Profile? = if (jsonObject.has("profile")) {
        Profile(jsonObject.getJSONObject("profile"))
    } else {
        null
    }

    /**
     * The user's decentralized identifier - this is used to uniquely identify a user
     */
    val decentralizedID: String
        get() {
            return jsonObject.getString("decentralizedID")
        }

    /**
     * The user's private key for the currently logged in app
     */
    val appPrivateKey: String
        get() {
            return jsonObject.getString("appPrivateKey")
        }

    /**
     * The user's gaia storage location
     */
    val hubUrl: String
        get() = jsonObject.getString("hubUrl")

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() {
            return jsonObject
        }
}