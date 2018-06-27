package org.blockstack.android.sdk

import java.net.URI

data class BlockstackConfig(
        val appDomain: URI,
        val redirectURI: URI,
        val manifestURI: URI,
        val scopes: Array<Scope>
)