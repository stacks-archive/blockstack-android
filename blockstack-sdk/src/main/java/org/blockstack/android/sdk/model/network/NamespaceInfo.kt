package org.blockstack.android.sdk.model.network

import org.json.JSONObject

/**
 * Object containing information about a namespace including the pricing parameters and creation history.
 */
class NamespaceInfo(private val jsonObject: JSONObject) {

    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() {
            return jsonObject
        }
}