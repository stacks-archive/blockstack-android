package org.blockstack.android.sdk.model

import org.json.JSONObject

data class GaiaHubConfig(val urlPrefix: String, val address: String, val token: String, val server: String) {

    fun toJSON(): JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("url_prefix", urlPrefix)
        optionsObject.put("address", address)
        optionsObject.put("token", token)
        optionsObject.put("server", server)
        return optionsObject
    }
}
