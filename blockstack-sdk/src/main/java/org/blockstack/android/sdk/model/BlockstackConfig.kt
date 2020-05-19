package org.blockstack.android.sdk.model

import org.blockstack.android.sdk.BaseScope
import org.blockstack.android.sdk.Scope
import java.net.URI

/**
 * Configuration of the app using Blockstack
 *
 * @property appDomain the domain of the app, like `https://example.com`
 * @property redirectPath the redirect path relative to appDomain used after successful sign-in. This page should redirect
 * to the url defined in the AndroidManifest
 * @property manifestPath the manifest path relative to appDomain. This page is the same as for progressive web apps.
 * @property scopes the permissions that the app needs from the user
 * @property coreNode - override the default or user selected core node
 * @property authenticatorURL - the web-based fall back authenticator

 */
data class BlockstackConfig(
        val appDomain: URI,
        val redirectPath: String,
        val manifestPath: String,
        val scopes: Array<Scope>,
        val coreNode: String? = null,
        val authenticatorUrl: String? = null
)


/**
 * convenience method to build default configuration from the app domain
 *
 * @receiver the app domain without trailing slash
 * @param scopes the requested permissions
 * @param redirectPath the path of the redirection url that is appended to the app domain, needs leading slash, defaults to '/redirect'
 * @param manifestPath the path of the manifest url that is appended to the app domain, needs leading slash, defaults to `/manifest.json'
 */
fun String.toBlockstackConfig(scopes: Array<Scope> = arrayOf(BaseScope.StoreWrite.scope), redirectPath: String = "/redirect", manifestPath: String = "/manifest.json"): BlockstackConfig =
        BlockstackConfig(
                URI(this),
                redirectPath,
                manifestPath,
                scopes)