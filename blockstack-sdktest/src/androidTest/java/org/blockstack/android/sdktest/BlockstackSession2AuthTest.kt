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
import org.kethereum.extensions.toHexStringNoPrefix
import org.komputing.kbip44.BIP44Element
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
        assertThat(result?.json?.getString("appPrivateKey"), `is`("6b52c9c23cb75d5e420441929a473fa49772575520f583e3e03d2919ac663a3a"))
        assertThat(result?.json?.getJSONObject("profile")?.toString(), `is`("{}"))
    }

    @Test
    fun makeAuthResponseJ2V8ThenHandleAuthResponse() {
        // generated using the Seed Words from BlockstackSignInTest and helloblockstack.com
        val authResponseJ2V8 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiIyMDQyYjg0NS02ZTI3LTQzMzEtYWUxZi04ODM2ODA5NjU4NGUiLCJpYXQiOjE2MDEyOTEzMzcsImV4cCI6MTYwMTI5MzkyOSwicHJpdmF0ZV9rZXkiOiI3YjIyNjk3NjIyM2EyMjY2MzgzODM2MzUzMDY1NjYzMjM2MzEzMTY2MzUzNDMyNjU2NjM2MzE2NDYzNjEzNTY1MzEzNTM4NjUzMTM0MzEyMjJjMjI2NTcwNjg2NTZkNjU3MjYxNmM1MDRiMjIzYTIyMzAzMzYxMzk2NTM2NjMzODY1MzIzNDM3MzU2MTMwMzU2MTY2NjU2NjM2MzUzOTMyMzAzOTY1Mzg2MTM0Mzk2NDYzMzEzNTMyNjQ2MzM1NjIzNzMwNjE2MTM1MzEzNTM3MzMzNzMyMzAzNTMzMzIzMTMzMzQzMDMwNjY2NDM1MzUzNTM2MjIyYzIyNjM2OTcwNjg2NTcyNTQ2NTc4NzQyMjNhMjI2MzMwNjEzNjMwNjEzMTMwMzczMjY1NjQ2NTYyNjIzNjMxMzc2NTM1MzE2MjM1NjY2NTYzMzczODY2MzkzODMyMzg2MzYzMzkzMzM2MzIzMTMwMzAzMDM1NjE2MzY2MzYzNTYzMzQzOTMwNjIzMTM0MzQzMzYyMzEzMjYxMzU2MTYyMzg2MjM2MzQzMjY1NjQzOTMxMzgzMjM3MzkzMTYzNjE2MTMxMzYzNTM5MzMzODY0MzA2MTYzNjU2MTYxMzE2MTYyMzkzMTY0NjI2MTM4Mzg2NjY0Mzc2NTMyNjUzMzYzNjY2NTM3MzYzMTY1NjQzMTMxNjI2NTMyMzczOTYxMzQ2NDM3NjM2NjY2NjQ2NTMzMzQ2MzM3MzkzNDM1MzE2MTM0MzczOTMzMzUzNjM5NjY2MzM5NjMzNDM4NjYzNDIyMmMyMjZkNjE2MzIyM2EyMjY0NjQ2MjYzMzgzODMzMzAzMTM0MzgzMjYyMzczMDYzMzAzNjYxNjM2MzM3MzgzMDM2NjQzNTM4MzIzMjY0NjU2MTMzMzQzMzM0MzIzMTMzMzQ2MzMwMzM2MjM1MzIzOTMyNjEzNDM3NjQ2MTM2MzM2NTMzNjMzMDM3MzczNDM5MjIyYzIyNzc2MTczNTM3NDcyNjk2ZTY3MjIzYTc0NzI3NTY1N2QiLCJwdWJsaWNfa2V5cyI6WyIwM2U5M2FlNjVkNjY3NTA2MWExNjdjMzRiODMyMWJlZjg3NTk0NDY4ZTliMmRkMTljMDVhNjdhN2I0Y2FlZmEwMTciXSwicHJvZmlsZSI6e30sInVzZXJuYW1lIjoiIiwiZW1haWwiOiIiLCJwcm9maWxlX3VybCI6bnVsbCwiaHViVXJsIjoiaHR0cHM6Ly9odWIuYmxvY2tzdGFjay5vcmciLCJibG9ja3N0YWNrQVBJVXJsIjoiaHR0cHM6Ly9jb3JlLmJsb2Nrc3RhY2sub3JnIiwiYXNzb2NpYXRpb25Ub2tlbiI6bnVsbCwidmVyc2lvbiI6IjEuMy4xIiwiaXNzIjoiZGlkOmJ0Yy1hZGRyOjFKZVRRNWNRanNENTdZR2NzVkZod1Q3aXVRVVhKUjZCU2sifQ.U3ixrAjOE8FEcW4xMw1ZqAdfdX_51CY8vfze5-cyiiFIJ1KNRXQ_SXV3NAyEqycDYdBir2JHFlvRFtYD-qIGYA"
        var result: UserData? = null
        runBlocking {
            sessionStore.setTransitPrivateKey("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
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

