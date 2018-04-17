package org.blockstack.android.sdk

import org.json.JSONException
import org.json.JSONObject

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

    val did: String
        get() {
            return jsonObject.getString("did")
        }

    val json: JSONObject
        get() {
            return jsonObject
        }
}