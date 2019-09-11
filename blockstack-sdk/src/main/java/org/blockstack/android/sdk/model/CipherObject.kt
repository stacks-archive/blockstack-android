package org.blockstack.android.sdk.model

import org.json.JSONObject

/**
 * Object containing encrypted content. It is backed by the JSONObject.
 *
 * The json object can be stored or transferred as string or file using
 * `json.toString()`. This an then be used for decrypting the content in @see decryptContent
 */
class CipherObject(private val jsonObject: JSONObject) {

    constructor (iv: String, ephemeralPK: String, cipherText: String, mac: String, wasString: Boolean) : this(JSONObject()
            .put("iv", iv)
            .put("ephemeralPK", ephemeralPK)
            .put("cipherText", cipherText)
            .put("mac", mac)
            .put("wasString", wasString))

    /**
     * json representation of the encrypted content
     *
     * json.toString should be used as parameter of decryptContent.
     *
     * @see BlockstackSession.decryptContent
     */
    val json: JSONObject
        get() {
            return jsonObject
        }

    val iv: String
        get() {
            return jsonObject.getString("iv")
        }
    val ephemeralPK: String
        get() {
            return jsonObject.getString("ephemeralPK")
        }
    val cipherText: String
        get() {
            return jsonObject.getString("cipherText")
        }
    val mac: String
        get() {
            return jsonObject.getString("mac")
        }
    val wasString: Boolean
        get() {
            return jsonObject.getBoolean("wasString")
        }
}
