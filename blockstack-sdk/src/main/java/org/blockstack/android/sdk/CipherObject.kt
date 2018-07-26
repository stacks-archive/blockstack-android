package org.blockstack.android.sdk

import org.json.JSONObject

/**
 * Object containing encrypted content. It is backed by the JSONObject.
 *
 * The json object can be stored or transferred as string or file using
 * `json.toString()`. This an then be used for decrypting the content in @see decryptContent
 */
class CipherObject(private val jsonObject: JSONObject) {
    val json: JSONObject
        get() {
            return jsonObject
        }
}
