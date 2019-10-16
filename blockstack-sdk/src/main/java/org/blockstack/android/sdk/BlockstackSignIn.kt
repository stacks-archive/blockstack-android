package org.blockstack.android.sdk

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.uport.sdk.jwt.JWTTools
import me.uport.sdk.jwt.model.JwtHeader
import me.uport.sdk.signer.KPSigner
import org.blockstack.android.sdk.Scope.Companion.scopesArrayToJSONString
import org.blockstack.android.sdk.model.BlockstackConfig
import org.blockstack.android.sdk.model.SessionData
import org.kethereum.crypto.CryptoAPI
import org.kethereum.crypto.toECKeyPair
import org.kethereum.extensions.toHexStringNoPrefix
import org.kethereum.model.PrivateKey
import java.util.*

class BlockstackSignIn(private val appConfig: BlockstackConfig, private val sessionStore: ISessionStore) {


    /**
     * Generates an authentication request that can be sent to the Blockstack browser
     * for the user to approve sign in. This authentication request can then be used for
     * sign in by passing it to the redirectToSignInWithAuthRequest method.
     *
     * Note: This method should only be used if you want to roll your own authentication flow.
     * Typically you'd use redirectToSignIn which takes care of this under the hood.
     *
     * @param transitPrivateKey hex encoded transit private key
     * @param expiresAt the time at which this request is no longer valid
     * @param extraParams key, value pairs that are transferred with the auth request,
     * only Boolean and String values are supported
     */
    suspend fun makeAuthRequest(transitPrivateKey: String, expiresAt: Long = Date().time + 3600 * 24 * 7, extraParams: Map<String, Any>? = null): String {

        val domainName = appConfig.appDomain.toString()
        val manifestUrl = "${domainName}${appConfig.manifestPath}"
        val redirectUrl = "${domainName}${appConfig.redirectPath}"
        val transitKeyPair = PrivateKey(transitPrivateKey).toECKeyPair()
        val btcAddress = transitKeyPair.toBtcAddress()
        val issuerDID = "did:btc-addr:${btcAddress}"
        val payload = mutableMapOf<String, Any>(
                "jti" to UUID.randomUUID().toString(),
                "iat" to Date().time / 1000,
                "exp" to expiresAt / 1000,
                "iss" to issuerDID,
                "public_keys" to arrayOf(transitKeyPair.toHexPublicKey64()),
                "domain_name" to domainName,
                "manifest_uri" to manifestUrl,
                "redirect_uri" to redirectUrl,
                "version" to "1.3.1",
                "do_not_include_profile" to true,
                "supports_hub_url" to true,
                "scopes" to scopesArrayToJSONString(appConfig.scopes)
        )
        if (extraParams != null) {
            payload.putAll(extraParams)
        }
        return JWTTools().createJWT(payload, issuerDID, KPSigner(transitPrivateKey), algorithm = JwtHeader.ES256K)
    }

    suspend fun redirectToSignIn(context: Context) {
        val transitPrivateKey = generateAndStoreTransitKey()
        val authRequest = makeAuthRequest(transitPrivateKey)
        redirectToSignInWithAuthRequest(context, authRequest, this.appConfig.authenticatorUrl)
    }

    /**
     * Redirects the user to the Blockstack browser to approve the sign in request.
     * To construct a request see the [[makeAuthRequest]] function.
     *
     * The user is redirected to the authenticator URL specified in the `AppConfig` or the default authenticator url
     *
     * @param authRequest A request string built by the [[makeAuthRequest]] function
     * @param blockstackIDHost The ID of the Blockstack Browser application.
     *
     */
    suspend fun redirectToSignInWithAuthRequest(context: Context, authRequest: String, blockstackIDHost: String? = null) {
        val hostUrl = blockstackIDHost ?: DEFAULT_BLOCKSTACK_ID_HOST
        val httpsURI = "${hostUrl}?authRequest=${authRequest}"
        openUrl(context, httpsURI)
    }

    fun generateAndStoreTransitKey(): String {
        val keyPair = CryptoAPI.keyPairGenerator.generate()
        val transitPrivateKey = keyPair.privateKey.key.toHexStringNoPrefix()
        sessionStore.sessionData = SessionData(sessionStore.sessionData.json
                .put("transitKey", transitPrivateKey))
        return transitPrivateKey
    }


    suspend fun openUrl(context: Context, location: String) {
        withContext(Dispatchers.Main) {
            val locationUri = Uri.parse(location)
            if (shouldLaunchInCustomTabs) {
                val builder = CustomTabsIntent.Builder()
                val options = BitmapFactory.Options()
                options.outWidth = 24
                options.outHeight = 24
                options.inScaled = true
                val backButton = BitmapFactory.decodeResource(context.resources, R.drawable.ic_arrow_back, options)
                builder.setCloseButtonIcon(backButton)
                builder.setToolbarColor(ContextCompat.getColor(context, R.color.org_blockstack_purple_50_logos_types))
                builder.setToolbarColor(ContextCompat.getColor(context, R.color.org_blockstack_purple_85_lines))
                builder.setShowTitle(true)
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(context, locationUri)
            } else {
                context.startActivity(Intent(Intent.ACTION_VIEW, locationUri).addCategory(Intent.CATEGORY_BROWSABLE))
            }
        }
    }

    companion object {
        val TAG = BlockstackSignIn::class.java.simpleName

        /**
         * Flag indicating whether the authentication flow should be started in custom tabs.
         * Defaults to true.
         *
         * Set this to false only if you can't use Verified App Links.
         */
        var shouldLaunchInCustomTabs = true

        /**
         * Flag indicating that verified app links should not be checked for correct configuration
         */
        var doNotVerifyAppLinkConfiguration = false

    }
}