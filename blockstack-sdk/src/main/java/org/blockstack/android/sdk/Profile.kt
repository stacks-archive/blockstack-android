package org.blockstack.android.sdk

import org.json.JSONObject

/**
 * Object containing user's profile. This object is backed by the original JSON representation.
 */
class Profile(private val jsonObject: JSONObject) {

    /**
     * If the profile is from a person then returns the full name
     */
    val name: String?
        get() {
            if (isPerson()) {
                return jsonObject.optString("name")
            }
            return null
        }

    /**
     * The short bio for person or a tag line for organizations
     */
    val description: String?
        get() {
            return jsonObject.optString("description")
        }

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() {
            return jsonObject
        }

    private fun isPerson() = "Person".equals(jsonObject.get("@type"))
}