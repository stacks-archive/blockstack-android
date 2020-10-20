package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import okio.ByteString
import okio.ByteString.Companion.encode
import org.blockstack.android.sdk.model.CryptoOptions
import org.blockstack.android.sdk.model.GaiaHubConfig
import org.blockstack.android.sdk.model.Hub
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"

@RunWith(AndroidJUnit4::class)
class HubTest {


    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var hub: Hub

    @Before
    fun setup() {
        hub = Hub()
    }

    @Test
    fun testGetFromGaiaHub() {

        val response = runBlocking {
            val hubConfig = hub.connectToGaia("https://hub.blockstack.org", PRIVATE_KEY, null)
            hub.uploadToGaiaHub("testGetFromGaiaHub.txt", "message".encode(Charsets.UTF_8), hubConfig)

            hub.getFromGaiaHub(hub.getFullReadUrl("testGetFromGaiaHub.txt", hubConfig))
        }
        assertThat(response.body?.string(), `is`("message"))
    }

}

