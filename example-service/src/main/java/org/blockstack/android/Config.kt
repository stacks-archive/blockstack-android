package org.blockstack.android

import org.blockstack.android.sdk.toBlockstackConfig

val defaultConfig = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(
        kotlin.arrayOf(org.blockstack.android.sdk.Scope.StoreWrite))