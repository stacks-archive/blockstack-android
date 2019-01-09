package org.blockstack.android.sdk.model

import org.json.JSONObject

class SessionData(private val jsonObject: JSONObject) {
    val json: JSONObject
        get() {
            return jsonObject
        }
}
