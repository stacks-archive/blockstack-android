package org.blockstack.android.sdk

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import org.blockstack.android.sdk.model.BlockstackConfig
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
private val DECENTRALIZED_ID = "did:btc-addr:1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"
private val BITCOIN_ADDRESS = "1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"

@RunWith(AndroidJUnit4::class)
class BlockstackSignInTest {
    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var signIn: BlockstackSignIn
    private lateinit var sessionStore: SessionStore
    private lateinit var config: BlockstackConfig

    @Before
    fun setup() {
        config = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray())
        sessionStore = sessionStoreforIntegrationTests(rule)
        signIn = BlockstackSignIn(
                config, sessionStore)
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
}