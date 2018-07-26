package org.blockstack.android.sdk

import java.net.URI

/**
 * Configuration of the app using Blockstack
 *
 * @property appDomain the domain of the app, like `https://example.com`
 * @property redirectURI the redirect url used after successful sign-in. This page should redirect
 * to the url defined in the AndroidManifest
 * @property manifestURI the manifest url. This page is the same as for progressive web apps.
 * @property scopes the permissions that the app needs from the user
 */
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