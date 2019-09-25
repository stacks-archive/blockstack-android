package org.blockstack.android.sdk

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import okhttp3.OkHttpClient
import org.blockstack.android.sdk.model.CryptoOptions
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch


private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
private val DECENTRALIZED_ID = "did:btc-addr:1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"
private val BITCOIN_ADDRESS = "1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"


@RunWith(AndroidJUnit4::class)
class BlockstackSession2EncryptionTest {
    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var session: BlockstackSession
    private lateinit var session2: BlockstackSession2

    @Before
    fun setup() {
        val sessionStore = sessionStoreforIntegrationTests(rule)
        val executor = IntegrationTestExecutor(rule)
        val callFactory = OkHttpClient()
        session = BlockstackSession(rule.activity,
                "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStore,
                executor = executor,
                callFactory = callFactory)

        // get a gaiaHubConfig by using a j2v8 call to gaia
        session.listFiles({ false }, {
            val gaiaHubConfig = sessionStore.sessionData.json.getJSONObject("userData").getJSONObject("gaiaHubConfig")
            Log.d(TAG, gaiaHubConfig.toString())
        })

        session2 = BlockstackSession2(sessionStore, executor, callFactory = callFactory)

        val appPrivateKey = sessionStore.sessionData.json.getJSONObject("userData").getString("appPrivateKey")
        val hubUrl = sessionStore.sessionData.json.getJSONObject("userData").getString("hubUrl")
        val associationToken = sessionStore.sessionData.json.getJSONObject("userData").optString("gaiaAssociationToken")
    }

    @Test
    fun testEncryptDecrypt2() {
        val message = "Hello Test"
        val result = session.encryptContent(message, CryptoOptions(publicKey = PUBLIC_KEY))
        val plainText = session2.decryptContent(result.value!!.json.toString(), false, CryptoOptions(privateKey = PRIVATE_KEY))
        assertThat(plainText.value as String, `is`(message))
    }


    @Test
    fun testEncrypt2Decrypt2() {
        val message = "Hello Test"
        val result = session2.encryptContent(message, CryptoOptions(publicKey = PUBLIC_KEY))
        val plainText = session2.decryptContent(result.value!!.json.toString(), false, CryptoOptions(privateKey = PRIVATE_KEY))
        assertThat(plainText.value as String, `is`(message))
    }


    @Test
    fun testEncrypt2Decrypt() {
        val message = "Hello Test"
        val result = session2.encryptContent(message, CryptoOptions(publicKey = PUBLIC_KEY))
        val plainText = session.decryptContent(result.value!!.json.toString(), false, CryptoOptions(privateKey = PRIVATE_KEY))
        assertThat(plainText.value as String, `is`(message))
    }

    companion object {
        val TAG = BlockstackSession2EncryptionTest::class.java.simpleName
    }
}