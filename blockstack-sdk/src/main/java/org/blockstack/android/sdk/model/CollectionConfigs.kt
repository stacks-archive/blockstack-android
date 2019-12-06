package org.blockstack.android.sdk.model

import org.json.JSONObject

class CollectionConfigs(val json: JSONObject) {
    fun get(collectionName: String): CollectionConfig? {
        return if (json.has(collectionName)) {
            CollectionConfig(json.getJSONObject(collectionName))
        } else {
            null
        }
    }

}

class CollectionConfig(val json: JSONObject) {
    val hubConfig: GaiaHubConfig
        get() {
            val hubConfig = json.getJSONObject("hubConfig")
            return GaiaHubConfig(hubConfig.getString("url_prefix"), hubConfig.getString("address"),
                    hubConfig.getString("token"), hubConfig.getString("server"))
        }
    val encryptionKey: String = json.getString("encryptionKey")
}
