package org.blockstack.android.sdk

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.blockstack.android.sdk.model.SessionData
import org.blockstack.android.sdk.model.UserData
import org.json.JSONObject

val BLOCKSTACK_SESSION = "blockstack_session"

private val EMPTY_DATA = "{}"
private val TAG = SessionStore::class.java.simpleName


interface ISessionStore {
    var sessionData: SessionData
    fun deleteSessionData()
    fun updateUserData(userData: UserData)

    fun setTransitPrivateKey(transitPrivateKey: String) {
        sessionData = SessionData(this.sessionData.json.put("transitKey", transitPrivateKey))
    }

    fun getTransitPrivateKey(): String {
        return sessionData.json.getString("transitKey")
    }
}


class SessionStore(private val prefs: SharedPreferences) : ISessionStore {
    private var sessionDataObject = SessionData(JSONObject(prefs.getString(BLOCKSTACK_SESSION, EMPTY_DATA)))
    override var sessionData: SessionData
        get() = sessionDataObject
        set(value) {
            Log.d(TAG, "set session data in store " + value.json.toString())
            sessionDataObject = value
            prefs.edit().putString(BLOCKSTACK_SESSION, value.json.toString()).apply()
        }

    override fun deleteSessionData() {
        prefs.edit().putString(BLOCKSTACK_SESSION, EMPTY_DATA).apply()
        sessionDataObject = SessionData(JSONObject())
    }

    override fun updateUserData(userData: UserData) {
        sessionDataObject.json.put("userData", userData.json)
        prefs.edit().putString(BLOCKSTACK_SESSION, sessionDataObject.json.toString()).apply()
    }
}

fun Context.getBlockstackSharedPreferences(): SharedPreferences =
        getSharedPreferences("${packageName}_blockstack_prefs", Context.MODE_PRIVATE)