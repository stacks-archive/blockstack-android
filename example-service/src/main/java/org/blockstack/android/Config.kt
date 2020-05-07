package org.blockstack.android

import org.blockstack.android.sdk.AppDetails
import org.blockstack.android.sdk.BaseScope.StoreWrite
import org.blockstack.android.sdk.model.toBlockstackConfig

val defaultConfig = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(
        arrayOf(StoreWrite.scope))

val defaultAppDetails = AppDetails("Hello Blockstack", "https://helloblockstack.com/icon-192x192.png")
