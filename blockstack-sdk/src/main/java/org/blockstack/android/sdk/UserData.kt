package org.blockstack.android.sdk

import org.json.JSONException
import org.json.JSONObject

/**
 * Object containing user data. This object is backed by the original JSON representation.
 *
 * This currently provides very minimal functionality. To access the full array of values,
 * you should parse the `json` property of this object or send a pull request to add additional
 * functionality to this class.
 */
class UserData(private val jsonObject: JSONObject) {

    // TODO move this into a profile property
    val avatarImage: String?
        get() {
            try {
                return jsonObject.getJSONObject("profile")
                        .getJSONArray("image")
                        .getJSONObject(0) // TODO iterator through images for avatar type
                        .getString("contentUrl")
            } catch (e: JSONException) {
                return null
            }
        }

    /**
     * The user's decentralized identifier - this is used to uniquely identify a user
     */
    val did: String
        get() {
            return jsonObject.getString("did")
        }

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() {
            return jsonObject
        }
}