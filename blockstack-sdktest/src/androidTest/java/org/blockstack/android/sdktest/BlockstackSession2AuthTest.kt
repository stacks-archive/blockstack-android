package org.blockstack.android.sdktest

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import me.uport.sdk.jwt.JWTTools
import okhttp3.OkHttpClient
import org.blockstack.android.sdk.*
import org.blockstack.android.sdk.model.*
import org.blockstack.android.sdktest.test.TestActivity
import org.blockstack.android.sdktest.j2v8.BlockstackSessionJ2V8
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
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

        appConfig = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
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
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, emptyMap())
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
            BlockstackSignIn(sessionStore, appConfig).makeAuthRequest(TRANSIT_PRIVATE_KEY, expiresAt, emptyMap())
        }
        val authResponse = runBlocking {
            val account = BlockstackAccount(null, keys, identity.salt)
            blockstack.makeAuthResponse(account, authRequest)
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
        val authResponseJ2V8 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiIyMzQ5YWE3YS01ZDZiLTQwYzctYjM3Zi0wMzM5YTI4MmJhMjAiLCJpYXQiOjE1Njk5NDQ2NjUsImV4cCI6MTU3MjYyNjY2NCwiaXNzIjoiZGlkOmJ0Yy1hZGRyOjFKZVRRNWNRanNENTdZR2NzVkZod1Q3aXVRVVhKUjZCU2siLCJwcml2YXRlX2tleSI6IjdiMjI2OTc2MjIzYTIyNjU2MjY0MzkzMDMyMzYzMjY0Mzk2MzM2MzY2MTM4MzQ2MjM0MzYzNDM5MzAzMDM1MzUzNDY1MzY2MTMyMzA2NDIyMmMyMjY1NzA2ODY1NmQ2NTcyNjE2YzUwNGIyMjNhMjIzMDMyNjQzMjM2MzM2MTMyMzI2MjM2MzYzMzM3MzA2NTY1MzkzMTY1NjQzMDM1MzU2MjMyMzYzNzMwNjQ2MTY2MzMzMDMwNjQ2MzY0NjEzMjMyMzg2MTM0MzMzNjMwNjYzNTYxMzk2MTMyMzEzMTY2NjQ2MTYyMzI2NDMwNjE2NDMxNjEyMjJjMjI2MzY5NzA2ODY1NzI1NDY1Nzg3NDIyM2EyMjMxMzEzNTMwNjMzMTM4MzYzMTMyMzgzMzM3NjM2MjY1MzkzNjMyMzg2NDYzMzc2MjYyMzUzMjM4MzYzNDYyMzYzNjY2Mzk2NDY0MzYzOTMxMzkzMzMzMzUzNDY2NjU2MjYzMzAzNjMwMzA2NTM3NjE2NjMyNjE2MTM5Mzc2NjY0MzAzOTM1MzAzNTY0Mzg2MjM4NjMzMTM4NjY2MTM4Mzk2MjMwNjQzNjY2NjIzMTMzMzY2NjY0NjI2NDMyNjMzMDM3NjY2MTYxNjY2MzMwNjEzMTM4Mzc2MTYyMzEzMDMxNjQ2NTYzMzUzNjYyNjEzOTYxNjIzMzM4MzQ2NjYyNjI2NDY2NjQzMDMzMzU2MzY0NjE2MjYzMzEzNzY2MzA2NjMwMzM2MzMwMzUzNzY1MzMzMjM5MzQzODY2Mzg2MTM5MjIyYzIyNmQ2MTYzMjIzYTIyMzI2NTY0NjE2NDM4NjM2NTY1NjE2MzMxMzg2NDYzMzkzMTM4Mzk2MzM0MzMzOTYyNjQ2MjYyMzgzMDM2NjEzMTMzMzk2NjMzMzIzMDMzNjEzOTY2NjYzMjM4NjQ2MzM5NjUzNDMwNjM2MzMzNjMzNzYxNjMzNjY2NjMzODYzNjEyMjJjMjI3NzYxNzM1Mzc0NzI2OTZlNjcyMjNhNzQ3Mjc1NjU3ZCIsInB1YmxpY19rZXlzIjpbIjAzZTkzYWU2NWQ2Njc1MDYxYTE2N2MzNGI4MzIxYmVmODc1OTQ0NjhlOWIyZGQxOWMwNWE2N2E3YjRjYWVmYTAxNyJdLCJwcm9maWxlIjpudWxsLCJ1c2VybmFtZSI6InB1YmxpY19wcm9maWxlX2Zvcl90ZXN0aW5nLmlkLmJsb2Nrc3RhY2siLCJjb3JlX3Rva2VuIjpudWxsLCJlbWFpbCI6bnVsbCwicHJvZmlsZV91cmwiOiJodHRwczovL2dhaWEuYmxvY2tzdGFjay5vcmcvaHViLzFKZVRRNWNRanNENTdZR2NzVkZod1Q3aXVRVVhKUjZCU2svcHJvZmlsZS5qc29uIiwiaHViVXJsIjoiaHR0cHM6Ly9odWIuYmxvY2tzdGFjay5vcmciLCJibG9ja3N0YWNrQVBJVXJsIjpudWxsLCJhc3NvY2lhdGlvblRva2VuIjpudWxsLCJ2ZXJzaW9uIjoiMS4zLjEifQ.lozp1p_UyPhoRLP89MUnQ9IlCp5rchWvvB4r_-XZCoLcgPIhq5A2sFwf1MZPN4FOOLuIT23JX2WHQp0Relib8w"
        val latch = CountDownLatch(1)
        var result: UserData? = null
        runBlocking {
            sessionStore.setTransitPrivateKey("602f0c6d2ea9a6318063c5dcaa1add5686a78a641efa9875b32c62b0716d7a63")
            session.handlePendingSignIn(authResponseJ2V8) {
                Log.d(TAG, it.error?.toString() + " " + it.value)
                result = it.value
                latch.countDown()
            }
        }

        latch.await()

        assertThat(result?.json?.getString("decentralizedID"), `is`("did:btc-addr:1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk"))
        assertThat(result?.json?.getString("appPrivateKey"), `is`("a8025a881da1074b012995beef7e7ccb42fea2ec66e62367c8d73734033ee33b"))
    }

    companion object {
        val TAG = BlockstackSession2AuthTest::class.java.simpleName
    }
}

