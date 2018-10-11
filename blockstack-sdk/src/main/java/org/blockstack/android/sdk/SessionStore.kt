package org.blockstack.android.sdk

import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject

val BLOCKSTACK_SESSION = "blockstack_session"

private val EMPTY_DATA = "{}"
private val TAG = SessionStore::class.java.simpleName

class SessionStore(private val prefs: SharedPreferences) {
    var sessionDataObject = SessionData(JSONObject(prefs.getString(BLOCKSTACK_SESSION, EMPTY_DATA)))
    var sessionData: SessionData
        get() = sessionDataObject
        set(value) {
            Log.d(TAG, "set session data in store " + value.json.toString())
            sessionDataObject = value
            prefs.edit().putString(BLOCKSTACK_SESSION, value.json.toString()).apply()
        }

    fun deleteSessionData() {
        prefs.edit().putString(BLOCKSTACK_SESSION, EMPTY_DATA).apply()
        sessionDataObject = SessionData(JSONObject())
    }

}