package org.blockstack.android.sdk

import org.json.JSONObject

class NameInfo(private val jsonObject: JSONObject) {
    /**
     * The `JSONObject` that backs this object. You use this object to
     * access properties that are not yet exposed by this class.
     */
    val json: JSONObject
        get() {
            return jsonObject
        }
}