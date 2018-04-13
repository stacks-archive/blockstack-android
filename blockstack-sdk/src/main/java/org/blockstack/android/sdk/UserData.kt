package org.blockstack.android.sdk

import org.json.JSONException
import org.json.JSONObject

class UserData(private val jsonObject: JSONObject) {
    fun getAvatarImage(): String? {
        try {
            return jsonObject.getJSONObject("profile")
                    .getJSONArray("image")
                    .getJSONObject(0) // TODO iterator through images for avatar type
                    .getString("contentUrl")
        } catch (e: JSONException) {
            return null
        }
    }

    fun getDid(): String {
         return jsonObject.getString("did")
    }
}