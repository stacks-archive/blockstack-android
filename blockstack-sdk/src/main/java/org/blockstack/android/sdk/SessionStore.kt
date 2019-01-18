package org.blockstack.android.sdk

import android.content.SharedPreferences
import android.util.Log
import org.blockstack.android.sdk.model.SessionData
import org.json.JSONObject

val BLOCKSTACK_SESSION = "blockstack_session"

private val EMPTY_DATA = "{}"
private val TAG = SessionStore::class.java.simpleName

/**
 * Interface giving access to stored Blockstack session data.
 */
interface ISessionStore {
    var sessionData: SessionData
    fun deleteSessionData()
}

/**
 * Object giving access to stored Blockstack session data. The data is stored in
 * the provided preferences.
 *
 * @param prefs shared preferences used to store the data.
 */
class SessionStore(private val prefs: SharedPreferences) : ISessionStore {
    override var sessionData: SessionData
        get() {
            return SessionData(JSONObject(prefs.getString(BLOCKSTACK_SESSION, EMPTY_DATA)))
        }
        set(value) {
            prefs.edit().putString(BLOCKSTACK_SESSION, value.json.toString()).apply()
        }

    override fun deleteSessionData() {
        prefs.edit().putString(BLOCKSTACK_SESSION, EMPTY_DATA).apply()
    }
}