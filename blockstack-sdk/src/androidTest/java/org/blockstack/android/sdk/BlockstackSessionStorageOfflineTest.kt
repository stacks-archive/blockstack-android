package org.blockstack.android.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import org.blockstack.android.sdk.model.GetFileOptions
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch


@RunWith(AndroidJUnit4::class)
class BlockstackSessionStorageOfflineTest {
    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var session: BlockstackSession

    @Before
    fun setup() {
        val realCallFactory = OkHttpClient()
        val callFactory = Call.Factory {
            if (it.url().encodedPath().contains("/hub_info")) {
                realCallFactory.newCall(it)
            } else {
                throw IOException("offline")
            }
        }

        session = BlockstackSession(
                appConfig = "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStoreforIntegrationTests(rule),
                callFactory = callFactory, blockstack = Blockstack())
        runBlocking {
            session.getOrSetLocalGaiaHubConnection()
        }
    }


    @Test
    fun testOfflineGetFile() {
        var result: Result<out Any>? = null

        if (session.isUserSignedIn()) {
            runBlocking {
                 result = session.getFile("404file.txt", GetFileOptions(false))

            }
        }
        assertThat(result, `is`(notNullValue()))
        assertThat(result?.value, `is`(nullValue()))
        assertThat(result?.error?.message, `is`("offline"))
    }
}