package org.blockstack.android.sdk.extensions

import org.json.JSONObject
import java.util.*

fun JSONObject.getStringOrNull(key: String): String? {
    return if(!isNull(key) && optString(key).toUpperCase(Locale.getDefault()) != "NULL") {
        optString(key)
    } else {
        null
    }
}