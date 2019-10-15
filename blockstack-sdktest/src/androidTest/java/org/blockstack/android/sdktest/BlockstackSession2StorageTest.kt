package org.blockstack.android.sdktest

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.uport.sdk.jwt.JWTTools
import okhttp3.OkHttpClient
import org.blockstack.android.sdk.Blockstack
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.model.GetFileOptions
import org.blockstack.android.sdk.model.PutFileOptions
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.blockstack.android.sdktest.j2v8.BlockstackSessionJ2V8
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.komputing.khex.extensions.toHexString
import java.util.concurrent.CountDownLatch


private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
private val DECENTRALIZED_ID = "did:btc-addr:1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"
private val BITCOIN_ADDRESS = "1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"


@RunWith(AndroidJUnit4::class)
class BlockstackSession2StorageTest {

    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var session: BlockstackSessionJ2V8
    private lateinit var blockstack: Blockstack
    private lateinit var session2: BlockstackSession

    @Before
    fun setup() {
        val sessionStore = org.blockstack.android.sdk.sessionStoreforIntegrationTests(rule)
        val executor = IntegrationTestExecutor(rule)
        val callFactory = OkHttpClient()
        session = BlockstackSessionJ2V8(rule.activity,
                "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStore,
                executor = executor,
                callFactory = callFactory)

        // get a gaiaHubConfig by using a j2v8 call to gaia
        session.listFiles({ false }, {
            val gaiaHubConfig = sessionStore.sessionData.json.getJSONObject("userData").getJSONObject("gaiaHubConfig")
            Log.d(TAG, gaiaHubConfig.toString())
        })

        blockstack = Blockstack()
        session2 = BlockstackSession(sessionStore, callFactory = callFactory, blockstack = blockstack)

        val appPrivateKey = sessionStore.sessionData.json.getJSONObject("userData").getString("appPrivateKey")
        val hubUrl = sessionStore.sessionData.json.getJSONObject("userData").getString("hubUrl")
        val associationToken = sessionStore.sessionData.json.getJSONObject("userData").optString("gaiaAssociationToken")

        val latch = CountDownLatch(1)
        GlobalScope.launch {
            session2.gaiaHubConfig = session2.connectToGaia(hubUrl, appPrivateKey, associationToken)
            Log.d(BlockstackSession.TAG, session2.gaiaHubConfig.toString())
            latch.countDown()
        }
        latch.await()

        val triple = JWTTools().decode("eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJnYWlhQ2hhbGxlbmdlIjoiW1wiZ2FpYWh1YlwiLFwiMFwiLFwic3RvcmFnZTIuYmxvY2tzdGFjay5vcmdcIixcImJsb2Nrc3RhY2tfc3RvcmFnZV9wbGVhc2Vfc2lnblwiXSIsImh1YlVybCI6Imh0dHBzOi8vaHViLmJsb2Nrc3RhY2sub3JnIiwiaXNzIjoiMDI0NjM0ZWUxZDRmZjU3ZjJlMGVjN2E4NDdlMTcwNWVjNTYyOTQ5Zjg0YTgzZDFmNWZkYjU5NTYyMjBhOTc3NWUwIiwic2FsdCI6ImMzYjliNGFlZmFkMzQzZjIwNGJkOTVjMmZhMWFlMGE0In0.CZfrO3SS7f0UHNxmQH4cQuOX3ShOJqbqFOYtcSF58KyaXAUXw_CUClyXw6o4hQMb6jWVJSTUi7QB_qoY672nuw")
        Log.d(TAG, "header: " + triple.first.toJson())
        Log.d(TAG, triple.second.toString())
        Log.d(TAG, triple.third.toHexString())
    }

    @Test
    fun testPutGet2EncryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (session.isUserSignedIn()) {

            session.putFile("try.txt", "Hello Test", PutFileOptions(true)) {
                runBlocking {
                    session2.getFile("try.txt", GetFileOptions(true)) {
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
    fun testPut2GetEncryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (session.isUserSignedIn()) {
            runBlocking {
                session2.putFile("try.txt", "Hello Test", PutFileOptions(true)) {
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
    fun testPut2Get2EncryptedStringFile() {
        var result: String? = null
        val latch = CountDownLatch(1)

        if (session.isUserSignedIn()) {
            runBlocking {
                session2.putFile("try.txt", "Hello Test", PutFileOptions(true)) {
                    runBlocking {
                        session2.getFile("try.txt", GetFileOptions(true)) {
                            if (it.value is String) {
                                result = it.value as String
                            }
                            latch.countDown()
                        }
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
    fun testListFiles() {
        var fileCount: Int? = null
        val latch = CountDownLatch(1)
        runBlocking {
            session2.putFile("try.text", "Hello Test", PutFileOptions()) {
                fileCount = runBlocking {
                    session2.listFiles {
                        true
                    }
                }
                latch.countDown()
            }
        }

        latch.await()
        assertThat(fileCount, `is`(Matchers.greaterThanOrEqualTo(1)))
    }
    companion object {
        val TAG = BlockstackSession2StorageTest::class.java.simpleName
    }
}