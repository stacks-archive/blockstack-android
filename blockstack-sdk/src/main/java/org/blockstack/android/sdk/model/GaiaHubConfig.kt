package org.blockstack.android.sdk.model

import org.json.JSONObject

data class GaiaHubConfig(val urlPrefix: String, val address: String, val token: String, val server: String) {
    val json: JSONObject
        get() = JSONObject()
                .put("url_prefix", urlPrefix)
                .put("address", address)
                .put("token", token)
                .put("server", server)
}
