package org.blockstack.android.sdktest

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import me.uport.sdk.jwt.JWTTools
import okhttp3.OkHttpClient
import org.blockstack.android.sdk.Blockstack
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.BlockstackSignIn
import org.blockstack.android.sdk.SessionStore
import org.blockstack.android.sdk.model.*
import org.blockstack.android.sdktest.j2v8.BlockstackSessionJ2V8
import org.blockstack.android.sdktest.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
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
import java.util.*
import java.util.concurrent.CountDownLatch


private val SEED_PHRASE = "sound idle panel often situate develop unit text design antenna vendor screen opinion balcony share trigger accuse scatter visa uniform brass update opinion media"
private val BTC_ADDRESS = "1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk"
private val TRANSIT_PRIVATE_KEY = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"


@RunWith(AndroidJUnit4::class)
class BlockstackSession2AuthTest {


    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var sessionStore: SessionStore
    private lateinit var sessionJ2V8: BlockstackSessionJ2V8
    private lateinit var blockstack: Blockstack
    private lateinit var session: BlockstackSession
    private lateinit var appConfig: BlockstackConfig
    private lateinit var identity: BlockstackIdentity
    private lateinit var keys: ExtendedKey
    private lateinit var privateKey: String

    @Before
    fun setup() {
        sessionStore = sessionStoreforIntegrationTests(rule)
        val executor = IntegrationTestExecutor(rule)
        val callFactory = OkHttpClient()
        val words = MnemonicWords(SEED_PHRASE)
        identity = BlockstackIdentity(words.toSeed().toKey("m/888'/0'"))
        keys = identity.identityKeys.generateChildKey(BIP44Element(true, 0))
        privateKey = keys.keyPair.privateKey.key.toHexStringNoPrefix()

        appConfig = "https://flamboyant-darwin-d11c17.netlify.app".toBlockstackConfig(emptyArray())
        sessionJ2V8 = BlockstackSessionJ2V8(rule.activity,
                appConfig,
                sessionStore = sessionStore,
                executor = executor,
                callFactory = callFactory)
        val latch = CountDownLatch(1)
        // get a gaiaHubConfig by using a j2v8 call to gaia
        sessionJ2V8.listFiles({ false }, {
            val gaiaHubConfig = sessionStore.sessionData.json.getJSONObject("userData").getJSONObject("gaiaHubConfig")
            Log.d(TAG, gaiaHubConfig.toString())
            latch.countDown()
        })
        latch.await()

        blockstack = Blockstack()
        session = BlockstackSession(sessionStore, callFactory = callFactory, appConfig = appConfig, blockstack = blockstack)
    }

    @Test
    fun testMakeAuthRequestEqualsMakeAuthRequestJ2V8() {

        val expiresAt = Date().time + 3600 * 24 * 7
        val authRequestJ2V8 = sessionJ2V8.makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, emptyMap())
        val authRequest = runBlocking {
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, false, emptyMap())
        }
        val token = JWTTools().decodeRaw(authRequestJ2V8)
        val token2 = JWTTools().decodeRaw(authRequest)
        assertThat(token2.second["iss"], `is`(token.second["iss"]))
        assertThat(token2.second["public_keys"].toString(), `is`(token.second["public_keys"].toString()))
    }

    @Test
    fun testMakeAuthResponseThenHandlePendingLoginJ2V8() {
        val expiresAt = Date().time + 3600 * 24 * 7
        val authRequest = runBlocking {
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, false, emptyMap())
        }
        val authResponse = runBlocking {
            val account = BlockstackAccount(null, keys, identity.salt)
            blockstack.makeAuthResponse(account, authRequest, emptyArray())
        }


        val latch = CountDownLatch(1)
        var result: UserData? = null
        Log.d(TAG, authResponse)
        sessionStore.sessionData.json.remove("userData") // make sure there is no existing user session
        sessionJ2V8.handlePendingSignIn(authResponse) {
            Log.d(TAG, it.error?.toString() + " " + it.value)
            result = it.value
            latch.countDown()
        }
        latch.await()
        assertThat(result?.json?.getString("decentralizedID"), `is`("did:btc-addr:$BTC_ADDRESS"))
        assertThat(result?.json?.getString("appPrivateKey"), `is`("a8025a881da1074b012995beef7e7ccb42fea2ec66e62367c8d73734033ee33b"))
        assertThat(result?.json?.getJSONObject("profile")?.toString(), `is`("{}"))
    }

    @Test
    fun makeAuthResponseJ2V8ThenHandleAuthResponse() {
        // generated using the Seed Words from BlockstackSignInTest and helloblockstack.com
        val authResponseJ2V8 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiI2YzBiOTEwMC0yZDhlLTQ2OTgtYWVhNy00OWUyNjVjOWVjMjQiLCJpYXQiOjE1ODk5NzU5OTQsImV4cCI6MTU5MjY1NDM5NCwiaXNzIjoiZGlkOmJ0Yy1hZGRyOjFKZVRRNWNRanNENTdZR2NzVkZod1Q3aXVRVVhKUjZCU2siLCJwcml2YXRlX2tleSI6IjdiMjI2OTc2MjIzYTIyMzk2NjM5NjQzNDY0NjEzNDMzMzAzNzM1NjY2NjM0MzEzMTMwNjU2NTYyMzI2NTYyMzMzMDM0NjEzNzYyMzI2MTIyMmMyMjY1NzA2ODY1NmQ2NTcyNjE2YzUwNGIyMjNhMjIzMDMyMzczOTM3Mzg2MjM0MzYzODMxMzAzMzM2MzEzNzM2NjYzMTMzNjE2MjMyNjYzNDY0MzkzMDM1NjM2MzYxMzc2MjY0NjYzMDM1NjUzNzM1NjY2NDM3NjUzMjYyNjUzNzM0MzgzOTM2MzMzNTMzMzgzMzY0NjMzNzY1NjUzMjM2MzEyMjJjMjI2MzY5NzA2ODY1NzI1NDY1Nzg3NDIyM2EyMjM4NjI2NDM2MzYzNjYxMzAzNjM4Mzg2MzM1MzMzOTM2MzMzNzYyMzEzODM1NjYzODM5NjUzNTMxMzQzMTM4MzQzMjM4MzQzMDYxNjYzNTM2MzkzNTY2MzIzNzM4MzYzMzMzMzQzNTYzNjMzNzMyNjQzNjM4NjQzMjMyMzg2NTM0MzAzNjMxMzE2MzYxMzUzMTM2MzA2MjM5NjM2NjM5Mzg2NDYyNjMzMDY0MzY2MzMzNjIzMzY0NjQ2MjMyMzczNjYzMzEzODY0NjIzNjMwNjI2NTY1NjMzOTM0MzA2MTMwMzk2MjYzNjQzNjYxMzY2NTY2MzUzMTM5MzEzMDYzNjU2NTMxMzQzMjY0MzUzODY2NjM2NjYyNjIzNDMyMzI2NDM5MzQzMjMzNjMzMTY2MzUzODM4NjEzNzM3NjY2MzM0MjIyYzIyNmQ2MTYzMjIzYTIyNjY2NDMzNjQzMjY2Mzg2NjM3MzIzOTY1MzYzNzMyNjYzMzMyMzMzMTYxMzk2NTMxMzUzNDMwMzEzOTYyMzAzMjM2MzMzMjM4MzgzMDYzMzQ2NjM2MzAzMDM4NjEzNjYyMzk2MzMyMzAzNjMwNjU2NTYyMzQzODMxMzMzNjMzMzgyMjJjMjI3NzYxNzM1Mzc0NzI2OTZlNjcyMjNhNzQ3Mjc1NjU3ZCIsInB1YmxpY19rZXlzIjpbIjAzZTkzYWU2NWQ2Njc1MDYxYTE2N2MzNGI4MzIxYmVmODc1OTQ0NjhlOWIyZGQxOWMwNWE2N2E3YjRjYWVmYTAxNyJdLCJwcm9maWxlIjpudWxsLCJ1c2VybmFtZSI6InB1YmxpY19wcm9maWxlX2Zvcl90ZXN0aW5nLmlkLmJsb2Nrc3RhY2siLCJjb3JlX3Rva2VuIjpudWxsLCJlbWFpbCI6bnVsbCwicHJvZmlsZV91cmwiOiJodHRwczovL2dhaWEuYmxvY2tzdGFjay5vcmcvaHViLzFKZVRRNWNRanNENTdZR2NzVkZod1Q3aXVRVVhKUjZCU2svcHJvZmlsZS5qc29uIiwiaHViVXJsIjoiaHR0cHM6Ly9odWIuYmxvY2tzdGFjay5vcmciLCJibG9ja3N0YWNrQVBJVXJsIjoiaHR0cHM6Ly9jb3JlLmJsb2Nrc3RhY2sub3JnIiwiYXNzb2NpYXRpb25Ub2tlbiI6ImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSkZVekkxTmtzaWZRLmV5SmphR2xzWkZSdlFYTnpiMk5wWVhSbElqb2lNRE13TkRNNE9USmlaVEkxTUdVeE1HSTNZakl4WXprNE9UWTVObVJrTkdJM05ESmhabVk1WmpOaE5EbGtaVEJtTjJNeVl6YzNPRGM1TVdNNVpURTJZMk5rSWl3aWFYTnpJam9pTURObE9UTmhaVFkxWkRZMk56VXdOakZoTVRZM1l6TTBZamd6TWpGaVpXWTROelU1TkRRMk9HVTVZakprWkRFNVl6QTFZVFkzWVRkaU5HTmhaV1poTURFM0lpd2laWGh3SWpveE5qSXhOVEV4T1RrMExqWTVMQ0pwWVhRaU9qRTFPRGs1TnpVNU9UUXVOamtzSW5OaGJIUWlPaUkxWmpWallXWmhaRE15TTJZMU56VTVPREppTXpFd1lUTmxZak0xT0RnM01pSjkuOFRMV2JrRGpiTXVFcDc1QTkxMDhKQnNtOGJkRWkyZHZtUld5Zm05TFZjTTFzSFE4akRXN0g1Ykk0YkRaZU1Hc1QwdjdGRmpfby1rS0V5T3kzMTlKZFEiLCJ2ZXJzaW9uIjoiMS4zLjEifQ.qFBqifP8J1tgiSQwij8NX_Cxr9teroQ4ufz4cbC-ZLOOkHs_WCTTOuZmAAs93kKPJwhpjvYdnkE2DLhbGpwrfg"
        var result: UserData? = null
        runBlocking {
            sessionStore.setTransitPrivateKey("81b0b433747e22e6867abc17a1a1f3010251cb0e961d41787992e2b07629ea7c")
            val it = session.handlePendingSignIn(authResponseJ2V8)
            Log.d(TAG, it.error?.toString() + " " + it.value)
            result = it.value
        }
        assertThat("token expired, recreate one for this test and update the transient private key", blockstack.decodeToken(authResponseJ2V8).second.getLong("exp"), `is`(Matchers.greaterThan(Date().time / 1000)))
        assertThat(result?.json?.getString("decentralizedID"), `is`("did:btc-addr:1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk"))
        assertThat(result?.json?.getString("appPrivateKey"), `is`("d61fdcc94a70300b2caff490f95780410bb2f013375fc79cd01a338a82af0966"))
    }

    companion object {
        val TAG = BlockstackSession2AuthTest::class.java.simpleName
    }
}

