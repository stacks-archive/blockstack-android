package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.blockstack.android.sdk.model.*
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kethereum.bip32.generateChildKey
import org.kethereum.bip32.model.ExtendedKey
import org.kethereum.bip32.toKey
import org.kethereum.bip39.model.MnemonicWords
import org.kethereum.bip39.toSeed
import org.kethereum.bip44.BIP44Element
import org.kethereum.extensions.toHexStringNoPrefix
import java.security.InvalidParameterException
import java.util.*

private val SEED_PHRASE = "sound idle panel often situate develop unit text design antenna vendor screen opinion balcony share trigger accuse scatter visa uniform brass update opinion media"
private val TRANSIT_PRIVATE_KEY = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
private val BTC_ADDRESS = "1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk"

@RunWith(AndroidJUnit4::class)
class BlockstackSignInTest {

    private lateinit var identity: BlockstackIdentity
    private lateinit var blockstack: Blockstack
    private lateinit var appConfig: BlockstackConfig
    private lateinit var keys: ExtendedKey
    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var signIn: BlockstackSignIn
    private lateinit var sessionStore: SessionStore
    private lateinit var config: BlockstackConfig
    private lateinit var session: BlockstackSession

    @Before
    fun setup() {
        config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        sessionStore = sessionStoreforIntegrationTests(rule)
        signIn = BlockstackSignIn(
                sessionStore, config)

        val callFactory = OkHttpClient()
        val words = MnemonicWords(SEED_PHRASE)
        identity = BlockstackIdentity(words.toSeed().toKey("m/888'/0'"))
        keys = identity.identityKeys.generateChildKey(BIP44Element(true, 0))
        val privateKey = keys.keyPair.privateKey.key.toHexStringNoPrefix()
        appConfig = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        blockstack = Blockstack()
        session = BlockstackSession(sessionStore, callFactory = callFactory, appConfig = appConfig, blockstack = blockstack)
    }


    @Test
    fun testSalt() {
        val account = BlockstackAccount(null, keys, identity.salt)
        val expectedSalt = "c15619adafe7e75a195a1a2b5788ca42e585a3fd181ae2ff009c6089de54ed9e"
        assertThat(account.salt, CoreMatchers.`is`(expectedSalt))
    }

    @Test
    fun testOwnerAddress() {
        val account = BlockstackAccount(null, keys, identity.salt)
        assertThat(account.ownerAddress, CoreMatchers.`is`(BTC_ADDRESS))
    }

    @Test
    fun testAppsNode() {
        val account = BlockstackAccount(null, keys, identity.salt)
        val appsNode = account.getAppsNode()

        val origin = "https://amazing.app:443"
        val appNode = appsNode.getAppNode(origin)

        val expectedAppNodeAddress = "1A9NEhnXq5jDp9BRT4DrwadRP5jbBK896X"
        assertThat(appNode.keyPair.toBtcAddress(), CoreMatchers.`is`(expectedAppNodeAddress))
    }

    @Test
    fun generateAndStoreTransitKeyReturnsTheCorrectKey() {
        val key = signIn.generateAndStoreTransitKey()
        val storedKey = sessionStore.sessionData.json.getString("transitKey")
        assertThat(key, `is`(storedKey))
        val storedKey2 = sessionStore.getTransitPrivateKey()
        assertThat(key, `is`(storedKey2))
    }

    @Test
    fun makeAuthRequestReturnsValidRequestToken() {
        val key = signIn.generateAndStoreTransitKey()
        val authRequest = runBlocking {
            signIn.makeAuthRequest(key, Date(System.currentTimeMillis() + 3600000).time, mapOf("solicitGaiaHubUrl" to true))
        }
        assertThat(authRequest, Matchers.startsWith("ey"))

        val token = Blockstack().decodeToken(authRequest)
        val payload = token.second
        assertThat(payload, `is`(notNullValue()))
        assertThat(payload.optString("domain_name"), `is`(config.appDomain.toString()))
        assertThat(payload.optBoolean("solicitGaiaHubUrl"), `is`(true))
    }


    @Test
    fun testMakeAuthResponse2HandlePendingLogin2() {
        val expiresAt = Date().time + 3600 * 24 * 7
        val authRequest = runBlocking {
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, emptyMap())
        }
        val authResponse = runBlocking {
            val account = BlockstackAccount(null, keys, identity.salt)
            blockstack.makeAuthResponse(account, authRequest, emptyArray())
        }

        var result: UserData? = null
        runBlocking {
            sessionStore.setTransitPrivateKey(TRANSIT_PRIVATE_KEY)

            result = session.handlePendingSignIn(authResponse).value
        }

        assertThat(result?.json?.getString("decentralizedID"), CoreMatchers.`is`("did:btc-addr:1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk"))
        assertThat(result?.json?.getString("appPrivateKey"), CoreMatchers.`is`("a8025a881da1074b012995beef7e7ccb42fea2ec66e62367c8d73734033ee33b"))
    }


    @Test
    fun testVerifyAuthResponse() {
        val expiresAt = Date().time + 3600 * 24 * 7
        val authRequest = runBlocking {
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, emptyMap())
        }

        val result = runBlocking {
            blockstack.verifyAuthRequest(authRequest)
        }

        assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun testVerifyAuthResponseWithoutUsername() {
        val expiresAt = Date().time + 3600 * 24 * 7
        val authRequest = runBlocking {
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, emptyMap())
        }
        val authResponse = runBlocking {
            val account = BlockstackAccount(null, keys, identity.salt)
            blockstack.makeAuthResponse(account, authRequest, emptyArray())
        }
        val isValid = runBlocking {
            blockstack.verifyToken(authResponse)
        }
        assertThat(isValid, CoreMatchers.`is`(true))
    }


    @Test
    fun testVerifyAuthResponseWithUsername() {
        val expiresAt = Date().time + 3600 * 24 * 7
        val authRequest = runBlocking {
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, emptyMap())
        }
        val authResponse = runBlocking {
            val account = BlockstackAccount("public_profile_for_testing.id.blockstack", keys, identity.salt)
            blockstack.makeAuthResponse(account, authRequest, emptyArray())
        }
        val isValid = runBlocking {
            blockstack.verifyToken(authResponse)
        }
        assertThat(isValid, CoreMatchers.`is`(true))
    }

    @Test
    fun testVerifyAuthResponseWithWrongUsernameWithImage() {
        val expiresAt = Date().time + 3600 * 24 * 7
        val authRequest = runBlocking {
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, emptyMap())
        }
        val authResponse = runBlocking {
            val account = BlockstackAccount("friedger.id", keys, identity.salt)
            blockstack.makeAuthResponse(account, authRequest, emptyArray())
        }
        val isValid = runBlocking {
            blockstack.verifyToken(authResponse)
        }
        assertThat(isValid, CoreMatchers.`is`(false)) // public keys do not match username
    }


    @Test
    fun testVerifyAuthResponseWithWrongUsername() {
        val expiresAt = Date().time + 3600 * 24 * 7
        val authRequest = runBlocking {
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, emptyMap())
        }
        runBlocking {
            val account = BlockstackAccount("invalid$$.id.blockstack", keys, identity.salt)
            try {
                blockstack.makeAuthResponse(account, authRequest, emptyArray())
                throw RuntimeException("should have failed")
            } catch (e: InvalidParameterException) {
                assertThat(e.message, CoreMatchers.`is`("could not fetch name info 404"))
            }
        }
    }

    @Test
    fun testVerifyAuthUnencryptedAuthResponse() {
        val authResponse = runBlocking {
            val account = BlockstackAccount("public_profile_for_testing.id.blockstack", keys, identity.salt)
            blockstack.makeAuthResponseUnencrypted(account, "https://flamboyant-darwin-d11c17.netlify.com", emptyArray())
        }
        assertThat(authResponse, CoreMatchers.`is`(CoreMatchers.notNullValue()))
        val result = runBlocking {
            session.handleUnencryptedSignIn(authResponse)
        }
        assertThat(result.error, CoreMatchers.nullValue())
        assertThat(result.value?.decentralizedID, CoreMatchers.`is`("did:btc-addr:1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk"))
    }
}