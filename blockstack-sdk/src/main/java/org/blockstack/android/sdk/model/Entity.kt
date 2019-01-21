package org.blockstack.android.sdk.model

import org.json.JSONObject

/**
 * Object representing a person or organization
 */
class Entity(private val jsonObject: JSONObject) {
    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() = jsonObject
}