package org.blockstack.android.sdk

import org.json.JSONObject

class SessionData(private val jsonObject: JSONObject) {
    val json: JSONObject
        get() {
            return jsonObject
        }
}
