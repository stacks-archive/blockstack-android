package org.blockstack.android

import org.blockstack.android.sdk.Scope

val defaultConfig = java.net.URI("https://flamboyant-darwin-d11c17.netlify.com").run {
    org.blockstack.android.sdk.BlockstackConfig(
            this,
            java.net.URI("${this}/redirect"),
            java.net.URI("${this}/manifest.json"),
            kotlin.arrayOf(Scope.StoreWrite, Scope.Email))
}
