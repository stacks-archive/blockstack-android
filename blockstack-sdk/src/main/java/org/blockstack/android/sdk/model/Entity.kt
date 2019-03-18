package org.blockstack.android.sdk.model

import org.json.JSONObject

/**
 * Object representing a person or organization
 */
class Entity(private val jsonObject: JSONObject) {


    /**
     * The public key representing this entity
     */
    val publicKey: String = jsonObject.getString("publicKey")

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() = jsonObject

    companion object {
        fun withKey(publicKey: String): Entity {
            val json = JSONObject()
            json.put("publicKey", publicKey)
            return Entity(json)
        }
    }
}

