package org.blockstack.android

import android.content.Context
import org.blockstack.android.sdk.AppDetails
import org.blockstack.android.sdk.BaseScope
import org.blockstack.android.sdk.SessionStore
import org.blockstack.android.sdk.getBlockstackSharedPreferences
import org.blockstack.android.sdk.model.toBlockstackConfig

val defaultConfig = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(
        arrayOf(BaseScope.StoreWrite.scope, BaseScope.Email.scope))
val defaultAppDetails = AppDetails("Blockstack Demo Multi Activities", "https://helloblockstack.com/icon-192x192.png")

class SessionStoreProvider {
    companion object {
        private var instance: SessionStore? = null
        fun getInstance(context: Context): SessionStore {
            var sessionStore = instance
            if (sessionStore == null) {
                sessionStore = SessionStore(context.getBlockstackSharedPreferences())
                instance = sessionStore
            }
            return sessionStore
        }
    }
}
