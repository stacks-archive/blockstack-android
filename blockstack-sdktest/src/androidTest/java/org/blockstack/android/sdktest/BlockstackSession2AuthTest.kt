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
        val authResponseJ2V8 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiI3MjMyZTFjOC00ZDBjLTRmNzYtOTZlZi0wOGExZGM0MDk4ODkiLCJpYXQiOjE1NzU1NTIxOTYsImV4cCI6MTU3ODIzMDU5NiwiaXNzIjoiZGlkOmJ0Yy1hZGRyOjFKZVRRNWNRanNENTdZR2NzVkZod1Q3aXVRVVhKUjZCU2siLCJwcml2YXRlX2tleSI6IjdiMjI2OTc2MjIzYTIyNjEzMzYyMzI2MzYzMzMzOTM5MzI2NDM3MzUzNjMxMzk2NTYyNjEzMzYyMzU2NjM4Mzk2NjYyNjI2MjMzMzQzMjIyMmMyMjY1NzA2ODY1NmQ2NTcyNjE2YzUwNGIyMjNhMjIzMDMzNjQzNDM4NjIzMTYzNjIzMTM5MzYzNjM0MzQ2MTYyMzQzMjM0MzYzNjYzNjMzMDM5MzIzOTYxNjE2MzM4MzQzODMxMzE2MzM5Mzg2NjY2NjY2MzY1NjQ2NDM2NjQ2NTYxNjEzMjY0NjIzODM5NjEzODYxNjM2NjYyNjQzMTYxMzQyMjJjMjI2MzY5NzA2ODY1NzI1NDY1Nzg3NDIyM2EyMjMzNjQ2NjY2MzIzNzM0NjEzNzM2MzEzOTM5NjIzODY1MzIzMzYxNjQ2NDM2NjYzMjM0MzA2NjY0Mzc2MjM2MzkzODM4MzA2NTM5MzMzMzMyMzEzNDM2MzgzNjY1NjYzNTY0NjQzNDM4MzI2NTM2NjQ2MjMyNjEzMzMwNjYzOTM5NjY2NDM3MzAzMDYyNjQzMTMxNjEzNDM5NjUzNTM3NjQzNDMwMzEzMzYxNjI2NDMxMzMzOTY1MzczOTMwNjMzMzMwMzI2MTY1NjMzMDMyMzkzMzM1Mzk2MTY1MzAzMzYzMzM2MTMxMzAzNTYyMzYzNDYzMzAzNzMxNjIzODY2MzkzODMzNjMzNTYzMzkzNzM0MzE2MzYyMzAzODM3NjEzODMyMzc2MTYzNjEzMjM5NjI2NDYzNjEzNzY0NjMzNDM2MjIyYzIyNmQ2MTYzMjIzYTIyMzMzODMwNjEzMzY1Mzc2MTM0NjE2NjM0MzgzODM2MzU2MzM1MzQzNjMyNjQzMDYxNjQ2MjM5Mzg2MTM3NjE2NTYxMzY2NjYxNjEzMzYxMzczNDMzNjEzOTM1MzM2NjMzNjU2NTY1MzU2NDM1NjU2NTYyNjUzMDMzNjYzMzMxMzIyMjJjMjI3NzYxNzM1Mzc0NzI2OTZlNjcyMjNhNzQ3Mjc1NjU3ZCIsInB1YmxpY19rZXlzIjpbIjAzZTkzYWU2NWQ2Njc1MDYxYTE2N2MzNGI4MzIxYmVmODc1OTQ0NjhlOWIyZGQxOWMwNWE2N2E3YjRjYWVmYTAxNyJdLCJwcm9maWxlIjpudWxsLCJ1c2VybmFtZSI6InB1YmxpY19wcm9maWxlX2Zvcl90ZXN0aW5nLmlkLmJsb2Nrc3RhY2siLCJjb3JlX3Rva2VuIjpudWxsLCJlbWFpbCI6bnVsbCwicHJvZmlsZV91cmwiOiJodHRwczovL2dhaWEuYmxvY2tzdGFjay5vcmcvaHViLzFKZVRRNWNRanNENTdZR2NzVkZod1Q3aXVRVVhKUjZCU2svcHJvZmlsZS5qc29uIiwiaHViVXJsIjoiaHR0cHM6Ly9odWIuYmxvY2tzdGFjay5vcmciLCJibG9ja3N0YWNrQVBJVXJsIjoiaHR0cHM6Ly9jb3JlLmJsb2Nrc3RhY2sub3JnIiwiYXNzb2NpYXRpb25Ub2tlbiI6ImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSkZVekkxTmtzaWZRLmV5SmphR2xzWkZSdlFYTnpiMk5wWVhSbElqb2lNRE13TkRNNE9USmlaVEkxTUdVeE1HSTNZakl4WXprNE9UWTVObVJrTkdJM05ESmhabVk1WmpOaE5EbGtaVEJtTjJNeVl6YzNPRGM1TVdNNVpURTJZMk5rSWl3aWFYTnpJam9pTURObE9UTmhaVFkxWkRZMk56VXdOakZoTVRZM1l6TTBZamd6TWpGaVpXWTROelU1TkRRMk9HVTVZakprWkRFNVl6QTFZVFkzWVRkaU5HTmhaV1poTURFM0lpd2laWGh3SWpveE5qQTNNRGc0TVRrMkxqQTRNU3dpYVdGMElqb3hOVGMxTlRVeU1UazJMakE0TVN3aWMyRnNkQ0k2SWpZek56TTVaR1ppWkRWaE5qa3laR0l3WlRrNU56aGxOems0WVRVeVlqazBJbjAuVHM2ZjY3Yl8za2pyYnVkY3QxV3N2Z3VGNWlUZmxjZTlCOFQ5OFNvVVlRSkxKQnFmQUw1a1hvajJuUjFhNmVLeFpwQ2N4a0QtMlBsTGtiVVZUUDMtNUEiLCJ2ZXJzaW9uIjoiMS4zLjEifQ.SHxzMMYX14D_pmHP_RvGvvpkyPDXgJR8kkSnjLJI-wo3vB__5zV784mWwVT7Dh9Ee6jnJ7-qLnz-vmjmpP5gYQ"
        var result: UserData? = null
        runBlocking {
            sessionStore.setTransitPrivateKey("6802f47449ba844d18c952cc5918b97146098f5709f7e8db89af889bb5a6ccf6")
            val it = session.handlePendingSignIn(authResponseJ2V8)
            Log.d(TAG, it.error?.toString() + " " + it.value)
            result = it.value
        }
        assertThat("token expired", blockstack.decodeToken(authResponseJ2V8).second.getLong("exp"), `is`(Matchers.greaterThan(Date().time / 1000)))
        assertThat(result?.json?.getString("decentralizedID"), `is`("did:btc-addr:1JeTQ5cQjsD57YGcsVFhwT7iuQUXJR6BSk"))
        assertThat(result?.json?.getString("appPrivateKey"), `is`("d61fdcc94a70300b2caff490f95780410bb2f013375fc79cd01a338a82af0966"))
    }

    companion object {
        val TAG = BlockstackSession2AuthTest::class.java.simpleName
    }
}

