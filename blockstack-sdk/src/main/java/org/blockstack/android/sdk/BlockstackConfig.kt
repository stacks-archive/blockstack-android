package org.blockstack.android.sdk

import java.net.URI

data class BlockstackConfig(
        val appDomain: URI,
        val redirectURI: URI,
        val manifestURI: URI,
        val scopes: Array<Scope>
)


fun String.toBlockstackConfig(scopes: Array<Scope>, redirectPath: String = "/redirect", manifestPath: String = "/manifest.json"): BlockstackConfig =
    org.blockstack.android.sdk.BlockstackConfig(
            URI(this),
            URI("${this}${redirectPath}"),
            URI("${this}${manifestPath}"),
            scopes)