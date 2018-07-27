package org.blockstack.android.sdk

import org.json.JSONObject

/**
 * Object containing user's profile. This object is backed by the original JSON representation.
 */
class Profile(private val jsonObject: JSONObject) {

    /**
     * The name of the person of profile or null if not defined
     */
    val name: String?
        get() {
            return jsonObject.optString("name")
        }

    /**
     * The short bio for person or a tag line for organizations
     */
    val description: String?
        get() = jsonObject.optString("description")

    /**
     * The url of the avatar image or null if not defined
     */
    val avatarImage: String?
        get() {
            return jsonObject
                    .optJSONArray("image")
                    ?.optJSONObject(0) // TODO iterator through images for avatar type
                    ?.optString("contentUrl")
        }

    /**
     * The email address of the account of null if not defined or not authorized
     */
    val email: String?
        get() = jsonObject.optString("email")

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() = jsonObject

    /**
     * returns true if the account belongs to a person. This is defined by the schema.org type 'Person'
     */
    fun isPerson() = "Person".equals(jsonObject.opt("@type"))
}