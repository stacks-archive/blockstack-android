package org.blockstack.android.sdktest

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import okhttp3.OkHttpClient
import org.blockstack.android.sdk.Blockstack
import org.blockstack.android.sdk.BlockstackSession
import org.blockstack.android.sdk.model.CryptoOptions
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdktest.test.TestActivity
import org.blockstack.android.sdktest.j2v8.BlockstackSessionJ2V8
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"


@RunWith(AndroidJUnit4::class)
class BlockstackSession2EncryptionTest {

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
    }

    @Test
    fun testEncryptJ2V8ThenDecrypt() {
        val message = "Hello Test"
        val result = sessionJ2V8.encryptContent(message, CryptoOptions(publicKey = PUBLIC_KEY))
        val plainText = blockstack.decryptContent(result.value!!.json.toString(), false, CryptoOptions(privateKey = PRIVATE_KEY))
        assertThat(plainText.value as String, `is`(message))
    }



    @Test
    fun testEncryptThenDecryptJ2V8() {
        val message = "Hello Test"
        val result = blockstack.encryptContent(message, CryptoOptions(publicKey = PUBLIC_KEY))
        val plainText = sessionJ2V8.decryptContent(result.value!!.json.toString(), false, CryptoOptions(privateKey = PRIVATE_KEY))
        assertThat(plainText.value as String, `is`(message))
    }

    companion object {
        val TAG = BlockstackSession2EncryptionTest::class.java.simpleName
    }
}