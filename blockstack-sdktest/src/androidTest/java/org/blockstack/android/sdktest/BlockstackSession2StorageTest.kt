package org.blockstack.android.sdktest

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.blockstack.android.sdk.Blockstack
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.SessionStore
import org.blockstack.android.sdk.model.GetFileOptions
import org.blockstack.android.sdk.model.PutFileOptions
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdktest.j2v8.BlockstackSessionJ2V8
import org.blockstack.android.sdktest.j2v8.Executor
import org.blockstack.android.sdktest.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch


@RunWith(AndroidJUnit4::class)
class BlockstackSession2StorageTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var sessionJ2V8: BlockstackSessionJ2V8
    private lateinit var blockstack: Blockstack
    private lateinit var session: BlockstackSession

    @Before
    fun setup() {
        val sessionStore = sessionStoreforIntegrationTests(rule)
        val executor = IntegrationTestExecutor(rule)
        val callFactory = OkHttpClient()
        sessionJ2V8 = BlockstackSessionJ2V8(rule.activity,
                "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStore,
                executor = executor,
                callFactory = callFactory)

        // get a gaiaHubConfig by using a j2v8 call to gaia
        sessionJ2V8.listFiles({ false }, {
            val gaiaHubConfig = sessionStore.sessionData.json.getJSONObject("userData").getJSONObject("gaiaHubConfig")
            Log.d(TAG, gaiaHubConfig.toString())
        })

        blockstack = Blockstack()
        session = BlockstackSession(sessionStore, callFactory = callFactory, blockstack = blockstack)

        val appPrivateKey = sessionStore.sessionData.json.getJSONObject("userData").getString("appPrivateKey")
        val hubUrl = sessionStore.sessionData.json.getJSONObject("userData").getString("hubUrl")
        val associationToken = sessionStore.sessionData.json.getJSONObject("userData").optString("gaiaAssociationToken")

        val latch = CountDownLatch(1)
        GlobalScope.launch {
            session.gaiaHubConfig = session.connectToGaia(hubUrl, appPrivateKey, associationToken)
            Log.d(BlockstackSession.TAG, session.gaiaHubConfig.toString())
            latch.countDown()
        }
        latch.await()
    }

    @Test
    fun testPutJ2V8ThenGetEncryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (sessionJ2V8.isUserSignedIn()) {

            sessionJ2V8.putFile("try.txt", "Hello Test", PutFileOptions(true)) {
                runBlocking {
                    session.getFile("try.txt", GetFileOptions(true)) {
                        if (it.value is String) {
                            result = it.value as String
                        }
                        latch.countDown()
                    }
                }
            }
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result, `is`("Hello Test"))
    }

    @Test
    fun testPutThenGetJ2V8EncryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (sessionJ2V8.isUserSignedIn()) {
            runBlocking {
                session.putFile("try.txt", "Hello Test", PutFileOptions(true)) {
                }
            }
            sessionJ2V8.getFile("try.txt", GetFileOptions(true)) {
                if (it.value is String) {
                    result = it.value as String
                }
                latch.countDown()
            }
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result, `is`("Hello Test"))
    }

    @Test
    fun testPutThenGetJ2V8SignedEncryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (sessionJ2V8.isUserSignedIn()) {
            runBlocking {
                session.putFile("try.txt", "Hello Test", PutFileOptions(true, sign = true)) {

                }
            }
            sessionJ2V8.getFile("try.txt", GetFileOptions(true, verify = true)) {
                if (it.value is String) {
                    result = it.value as String
                }
                latch.countDown()
            }
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result, `is`("Hello Test"))
    }

    @Test
    fun testPutJ2V8ThenGetSignedEncryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (sessionJ2V8.isUserSignedIn()) {
            sessionJ2V8.putFile("try.txt", "Hello Test", PutFileOptions(true, sign = true)) {
                latch.countDown()
            }
            latch.await()
            runBlocking {
                sessionJ2V8.getFile("try.txt", GetFileOptions(true, verify = true)) {
                    if (it.value is String) {
                        result = it.value as String
                    }
                }
            }

        }
        assertThat(result, `is`("Hello Test"))
    }

    @Test
    fun testPutThenGetJ2V8SignedUnencryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (sessionJ2V8.isUserSignedIn()) {
            runBlocking {
                session.putFile("try.txt", "Hello Test", PutFileOptions(false, sign = true)) {

                }
            }
            sessionJ2V8.getFile("try.txt", GetFileOptions(false, verify = true)) {
                if (it.value is String) {
                    result = it.value as String
                }
                latch.countDown()
            }
        } else {
            latch.countDown()
        }
        latch.await()
        assertThat(result, `is`("Hello Test"))
    }


    @Test
    fun testPutJ2V8ThenGetSignedUnencryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (sessionJ2V8.isUserSignedIn()) {
            sessionJ2V8.putFile("try.txt", "Hello Test", PutFileOptions(false, sign = true)) {
                latch.countDown()
            }
            latch.await()
            runBlocking {
                sessionJ2V8.getFile("try.txt", GetFileOptions(false, verify = true)) {
                    if (it.value is String) {
                        result = it.value as String
                    }
                }
            }

        }
        assertThat(result, `is`("Hello Test"))
    }

    companion object {
        val TAG = BlockstackSession2StorageTest::class.java.simpleName
    }
}


class IntegrationTestExecutor(var rule: ActivityTestRule<*>) : Executor {
    override fun onMainThread(function: (Context) -> Unit) {
        function(rule.activity)
    }

    override fun onNetworkThread(function: suspend () -> Unit) {
        runBlocking {
            function()
        }
    }

    override fun onV8Thread(function: () -> Unit) {
        function()
    }
}

val A_VALID_BLOCKSTACK_SESSION_JSON = "{\"transitKey\":\"000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f\",\"userData\":{\"username\":null,\"profile\":{\"@type\":\"Person\",\"@context\":\"http:\\/\\/schema.org\",\"apps\":{\"https:\\/\\/app.graphitedocs.com\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1Fuzd6sJXqtXhgoFpb8W19Ao4BCG3EHQGf\\/\",\"https:\\/\\/app.misthos.io\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1CkGhNN71a1dpgXQjncunYZpXakJvwrpmc\\/\"},\"image\":[{\"@type\":\"ImageObject\",\"name\":\"avatar\",\"contentUrl\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\\/\\/avatar-0\"}],\"name\":\"fm\"},\"decentralizedID\":\"did:btc-addr:12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\",\"identityAddress\":\"12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\",\"appPrivateKey\":\"89f92476f13f5b173e53926ad7d6e22baf78c6b1dcdf200c38dc73d2bf47d43b\",\"coreSessionToken\":null,\"authResponseToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiI3MWI3NWY1Yi0xNDJkLTQxMzQtOGZkYy0zYWUyMWRmZTkzNjAiLCJpYXQiOjE1MzcxNjkxODYsImV4cCI6MTUzOTc2MTE4NSwiaXNzIjoiZGlkOmJ0Yy1hZGRyOjEyR3ByOWtZWEpKbjRmV2VjNG5SVndITFNyclRpZWV5d3QiLCJwcml2YXRlX2tleSI6IjdiMjI2OTc2MjIzYTIyMzEzOTMzMzM2NTM5MzYzNTY2NjMzNzM5NjM2MjYyMzAzNjY0NjMzMjM0MzIzNzY2NjY2MjYzMzczMjMyMzgzOTIyMmMyMjY1NzA2ODY1NmQ2NTcyNjE2YzUwNGIyMjNhMjIzMDMyMzQzMTY1MzY2NjYzMzEzODM0MzAzMDMyMzQzMTY0NjYzNzMzMzMzNTY1NjM2MjY2MzQzMzM0NjY2NDM5MzU2MjY0NjUzMjY2MzIzNTMxNjU2MzM3MzczNjMxMzQzNDM0MzAzMjY2NjUzNTM3NjE2MzYzMzA2MjY2MzQzNjYxMzQyMjJjMjI2MzY5NzA2ODY1NzI1NDY1Nzg3NDIyM2EyMjM5MzkzMTYxMzM2MzM1NjYzOTY2NjI2MzM2MzYzMzY2MzMzMDY0MzgzNzMxMzgzNzMyNjIzODM5MzMzMTYxMzMzMTMyNjUzMjM0NjM2MjM4MzAzNzM1NjQzODY0MzkzNzM1Mzg2MjM3NjEzMDY1NjMzMTM0Mzg2MTM2MzEzMjM2MzIzMjY1MzYzMjYyMzQ2NjY1NjYzNjMzNjMzMTYzNjYzMjMzNjEzMTM3MzM2NTM0NjUzOTMwMzIzMzM5MzQ2NDY1NjEzMTMxMzQzOTYzNjE2NTYxMzMzMzY2MzMzODY2NjIzMTMwMzczNTM0MzU2NDM1MzUzNDM3NjYzNjY2NjMzODMwMzg2MTM2MzIzNjM2MzAzMDMzNjMzNzM5MzMzNzM4MzYzMzM4MzMzNDMyNjEzNjYzMzMzOTM4MzM2MjM5MjIyYzIyNmQ2MTYzMjIzYTIyNjYzODMwMzQ2MjMzMzY2MjY0MzgzMjM2NjQzNTM3MzIzMzYxMzQzOTM4MzczOTYzMzAzMDYzNjQ2MzMzNjYzMDYxNjQzNzY0NjE2MTYyNjE2NDM0MzY2NDMzMzczNjY0MzUzMTYxNjI2NjYzNjYzMTM4MzM2MTM4MzAzNDMwMzgyMjJjMjI3NzYxNzM1Mzc0NzI2OTZlNjcyMjNhNzQ3Mjc1NjU3ZCIsInB1YmxpY19rZXlzIjpbIjAyODcyNmQyMGZhMTI4Yjc1OWViOGIwOWZjY2Y2ODU2ZTQzMjM1YmI5OTkxNjI0OWNmNjNhM2NiMjA0MTY0Y2I4ZSJdLCJwcm9maWxlIjpudWxsLCJ1c2VybmFtZSI6bnVsbCwiY29yZV90b2tlbiI6bnVsbCwiZW1haWwiOiJmcmllZGdlckBnbWFpbC5jb20iLCJwcm9maWxlX3VybCI6Imh0dHBzOi8vZ2FpYS5ibG9ja3N0YWNrLm9yZy9odWIvMTJHcHI5a1lYSkpuNGZXZWM0blJWd0hMU3JyVGllZXl3dC9wcm9maWxlLmpzb24iLCJodWJVcmwiOiJodHRwczovL2h1Yi5ibG9ja3N0YWNrLm9yZyIsInZlcnNpb24iOiIxLjIuMCJ9.QM8wx-EDjZrHk1ubK99divaejN5XAcCn_OkYAiPXQUNtzENUy8ogyrHzB4xC7mwfMckzFsMxEOdzwVW_Xcjqqg\",\"hubUrl\":\"https:\\/\\/hub.blockstack.org\"}}"

fun sessionStoreforIntegrationTests(rule: ActivityTestRule<*>): SessionStore {
    val prefs = PreferenceManager.getDefaultSharedPreferences(rule.activity)
    prefs.edit().putString("blockstack_session", A_VALID_BLOCKSTACK_SESSION_JSON)
            .apply()
    return SessionStore(prefs)
}