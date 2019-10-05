package org.blockstack.android.sdk

import android.content.Context
import android.util.Log
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

class BlockstackSignIn (private val appConfig: BlockstackConfig, private val sessionStore: ISessionStore) {


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
    suspend fun makeAuthRequest(transitPrivateKey: String, expiresAt: Long, extraParams: Map<String, Any>): String {

        val domainName = appConfig!!.appDomain.toString()
        val manifestUrl = "${domainName}${appConfig.manifestPath}"
        val redirectUrl = "${domainName}${appConfig.redirectPath}"
        val transitKeyPair = PrivateKey(transitPrivateKey).toECKeyPair()
        val btcAddress = transitKeyPair.toBtcAddress()
        val issuerDID = "did:btc-addr:${btcAddress}"
        val payload = mapOf(
                "jti" to UUID.randomUUID().toString(),
                "iat" to Date().time/1000,
                "exp" to expiresAt / 1000,
                "iss" to issuerDID,
                "public_keys" to arrayOf(transitKeyPair.toHexPublicKey64()),
                "domain_name" to domainName ,
                "manifest_uri" to manifestUrl,
                "redirect_uri" to redirectUrl,
                "version" to "1.3.1",
                "do_not_include_profile" to true,
                "supports_hub_url" to true,
                "scopes" to scopesArrayToJSONString(appConfig.scopes)

        )
        return JWTTools().createJWT(payload, issuerDID, KPSigner(transitPrivateKey), algorithm = JwtHeader.ES256K)
    }

    suspend fun redirectToSignIn(context: Context) {
        val keyPair = CryptoAPI.keyPairGenerator.generate()
        val transitPrivateKey = keyPair.privateKey.key.toHexStringNoPrefix()
        sessionStore.sessionData = SessionData(sessionStore.sessionData.json
                .put("blockstack-transit-private-key", transitPrivateKey))
        val authRequest = makeAuthRequest(transitPrivateKey, Date().time + 3600 * 24 * 7, emptyMap())
        Log.d(TAG, authRequest)
    }

    companion object {
        val TAG = BlockstackSignIn::class.java.simpleName
    }
}