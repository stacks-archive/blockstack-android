package org.blockstack.android.sdk.model

import org.json.JSONObject

/**
 * An object to configure options for `deleteFile` operations.
 *
 * @property wasSigned set to true if the file was originally signed
 * in order for the corresponding signature file to also be deleted.
 */
public class DeleteFileOptions(val wasSigned: Boolean = false, val gaiaHubConfig: GaiaHubConfig? = null) {

    /**
     * json representation of these options as used by blockstack.js
     */
    fun toJSON(): JSONObject {
        val optionsObject = JSONObject()
        optionsObject.put("wasSigned", wasSigned)
        return optionsObject
    }

    /**
     * string representation in json format used by blockstack.js
     */
    override fun toString(): String {
        return toJSON().toString()
    }

}